AudioQueryLooper {
  var s,main,n,minHopSize,
  <>a,<>buffer,<>bufferCopy,<>indices,<>bus,
  <>loopers,<>grainBufs,<>monoInputs,<>w,
  <>ctrl,defaultOut,defaultSend;

  *new { |s,main,minHopSize|
    ^super.newCopyArgs(s ? Server.default, main, main.n,minHopSize ? 2).init;
  }

  init {
    fork {
      "%: Initializing Class".format(this.class).postln;
      buffer = Array.fill(n, { Buffer.alloc(s, s.sampleRate * (n+1) * 2.0, 1) }); s.sync;
      bufferCopy = Array.fill(n, { Buffer.alloc(s, s.sampleRate * (n+1) * 2.0, 1) }); s.sync;
      indices = Array.fill(n, { Buffer.new(s) }); s.sync;
      bus = Array.fill(n, { Bus.audio(s, 1) }); s.sync;
      loopers = Array.newClear(n); s.sync;
      grainBufs = Array.newClear(n); s.sync;
      monoInputs = Array.newClear(n); s.sync;
      this.makeSynthDefs; s.sync;
      this.makeLooperSynths; s.sync;
      this.makeGUI; s.sync;
      defer({ this.setDefaultBuses() }, 0); s.sync;
    };
  }

  setDefaultBuses {
    defaultOut = [main.groups[\loopers], 'loopers'];
    defaultSend = [main.send1Buses[0], 'send0'];
    ctrl[\out].do {|n| n.object_(defaultOut).valueAction_(defaultOut) };
    ctrl[\send].do {|n| n.object_(defaultSend).valueAction_(defaultSend) };
  }

  makeSynthDefs {
    SynthDef("bufWriteOnce", { |bus, buf, r=1, ampThreshold=(-3.dbamp)|
      var amp, input, isPlaying, env, sig, start, end;
      input = In.ar(bus, 1);
      amp = Amplitude.kr(input);
      isPlaying = (amp>ampThreshold);
      env = EnvGen.ar(Env.asr(0.01,1,1), gate: isPlaying);
      start = 0; end = BufFrames.kr(buf);
      sig = RecordBuf.ar(input * env, buf, run: (env>0), loop: 0, doneAction: 2);
    }).add;

    SynthDef("fluidTrig", { |t_trig,amp=0,gate=1,b,i,rel=0.3,out=0,send|
      var dur, pos, sig, index, start, end;
      index = TRand.kr(0, BufFrames.kr(i) - 2, LorenzTrig.kr);
      start = BufRd.kr(1, i, index, 0);
      end = BufRd.kr(1, i, index+1, 0);
      dur = (end - start) / BufSampleRate.ir(b);
      pos = ((start + end) / 2) / BufSampleRate.ir(b);
      sig = TGrains2.ar(2, t_trig, b, 1, pos, dur, 0, 0.1, 0.03, 0.03); // pan is -1 to 1
      sig = Linen.kr(gate: gate, releaseTime: rel, doneAction: 0) * sig;
      sig = [Out.ar(out, sig * amp), Out.ar(send, sig)]
    }).add;
  }

  makeLooperSynths {
    var bundle;
    fork {
      n.do { |n|
        var looperCallback = {
          // TODO: add global toggle
          fork {
            grainBufs[n].set(\gate, 0); s.sync;
            buffer[n].copyData(bufferCopy[n], dstStartAt: 0, srcStartAt: 0, numSamples: -1); s.sync;
            this.loadOnsets(bufferCopy[n], indices[n]); s.sync;
            grainBufs[n].set(\gate, 1); s.sync;
            loopers[n] = Synth("bufWriteOnce", [bus: bus[n], buf: buffer[n]], main.looperGroup).onFree(looperCallback).value(n); s.sync;
          };
        };
        monoInputs[n] = Synth(\Mic, [\in, n, \out, bus[n], \inamp, 0, \amp, 1], main.inputGroup); s.sync;
        // Buffer UGen: no buffer data
        bundle = s.makeBundle(false, { loopers[n] = Synth("bufWriteOnce", [bus: bus[n], buf: buffer[n]], main.looperGroup); NodeWatcher.register(loopers[n])});
        s.listSendBundle(nil, bundle); s.sync;
        grainBufs[n] = Synth("fluidTrig", [b: bufferCopy[n], i: indices[n], out: main.groups[\loopers], send: main.send1Buses[0]], main.synthGroup); s.sync;
        loopers[n].onFree(looperCallback); s.sync;
      };
    };
  }

  makeGUI {
    var colours, c, r, v;

    defer {
      w = Window("Loopers", Rect(1000.0.rand, 1000.0.rand, 500.0, 500.0));
      w.userCanClose = false;
      w.background_( Color.new255(64, 224, 208) );

      c = ControlSpec(0,127,step: 1);
      ctrl = Dictionary.new;
      ctrl[\in] = { DragSink(w, Rect(0,100,380*2,40) ) }!n;
      ctrl[\monoinputs] = { Slider(w, Rect(50, 50, 50, 50) ) }!n;
      ctrl[\grainbufs] = { Slider(w, Rect(50, 50, 50, 50) ) }!n;
      ctrl[\learn1] = { Button(w, Rect(0, 0, 150, 120) ) }!n;
      ctrl[\learn2] = { Button(w, Rect(0, 0, 150, 120) ) }!n;
      ctrl[\learn3] = { Button(w, Rect(0, 0, 150, 120) ) }!n;
      ctrl[\showWaveform] = { Button(w, Rect(0, 0, 150, 120) ) }!n;
      ctrl[\playWaveform] = { Button(w, Rect(0, 0, 150, 120) ) }!n;
      ctrl[\trig] = { Button(w, Rect(0, 0, 150, 120) ) }!n;
      ctrl[\map] = { Button(w, Rect(0, 0, 150, 120) ) }!n;
      ctrl[\send] = { DragSink(w, Rect(0,100,380*2,40) ) }!n;
      ctrl[\out] = { DragSink(w, Rect(0,100,380*2,40) ) }!n;

      colours = (File.realpath(MixerGUI.class.filenameSymbol).dirname.withTrailingSlash ++ "Colours.scd").load.value;
      colours[\aqua].do { |n,i| ctrl[\monoinputs][i].background_(n) };
      colours[\coral].do { |n,i| ctrl[\grainbufs][i].background_(n) };

      ctrl[\in].do { |n,i| n.string_("In: %".format(i)) };
      ctrl[\in].do { |n,i| n.action_({ |ds|
        monoInputs[i].set(\in, ds.object[0]);
        ds.string_("In: %".format(ds.object[0].index))
        });
      };

      ctrl[\trig].do { |n,i| n.string_("Trig") };
      ctrl[\trig].do { |n,i| n.action_({
        grainBufs[i].set(\t_trig, 1);
        });
      };

      ctrl[\map].do { |n,i| n.string_("Map") };
      ctrl[\map].do { |n,i| n.action_({
        grainBufs[i].map(\t_trig, main.control.clockBuses[i]);
        });
      };

      ctrl[\send].do { |n,i| n.string_("Send: %".format(i)) };
      ctrl[\send].do { |n,i| n.action_({ |ds|
        grainBufs[i].set(\send, ds.object[0]);
        ds.string_("Send %".format(ds.object[1]))
        });
      };

      ctrl[\out].do { |n,i| n.string_("Out: %".format(i)) };
      ctrl[\out].do { |n,i| n.action_({ |ds|
        grainBufs[i].set(\out, ds.object[0]);
        ds.string_("Out %".format(ds.object[1]))
        });
      };

      ctrl[\monoinputs].do { |n,i| n.action_( { monoInputs[i].set(\inamp, n.value) } ) };
      ctrl[\grainbufs].do { |n,i| n.action_( { grainBufs[i].set(\amp, n.value) } ) };

      ctrl[\learn1].do { |n,i| n.action_( {
        main.gui.hid.learnEnabled = true;
        main.gui.hid.selectedUI = [ctrl[\monoinputs][i],'slider'];
      } ) };
      ctrl[\learn2].do { |n,i| n.action_( {
        main.gui.hid.learnEnabled = true;
        main.gui.hid.selectedUI = [ctrl[\grainbufs][i],'slider'];
      } ) };

      ctrl[\learn3].do { |n,i| n.action_( {
        main.gui.hid.learnEnabled = true;
        main.gui.hid.selectedUI = [ctrl[\grainbufs][i],'slider']; // TODO: gate/asr learn
      } ) };

      ctrl[\showWaveform].do { |n,i| n.string_("Show waveform") };
      ctrl[\showWaveform].do { |n,i| n.action_( { bufferCopy[i].plot } ) };

      ctrl[\playWaveform].do { |n,i| n.string_("Play waveform") };
      ctrl[\playWaveform].do { |n,i| n.action_( { bufferCopy[i].play} ) };

      w.layout = VLayout(
        HLayout(*ctrl[\in]),
        HLayout(*ctrl[\monoinputs]),
        HLayout(*ctrl[\learn1]),
        HLayout(*ctrl[\grainbufs]),
        HLayout(*ctrl[\learn2]),
        HLayout(*ctrl[\trig]),
        HLayout(*ctrl[\learn3]),
        HLayout(*ctrl[\map]),
        HLayout(*ctrl[\showWaveform]),
        HLayout(*ctrl[\playWaveform]),
        HLayout(*ctrl[\send]),
        HLayout(*ctrl[\out]),
      )

    }
  }

  loadOnsets { |b,i|
    var t;
    fork {
      t = Main.elapsedTime;
      FluidBufOnsetSlice.process(s, b, indices: i, threshold:0.5, minSliceLength: minHopSize).wait;
      ["load onsets", (Main.elapsedTime - t)].postln;
    }
  }

}
