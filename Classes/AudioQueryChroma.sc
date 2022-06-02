AudioQueryChroma : AudioQuery {
  var nchroma = 12;
  var nmaxchroma = 120;

  setup {
    "%: Initializing Class".format(this.class).postln;
    corpus_indices_buf = Buffer(s); s.sync;
    test_sound = Buffer.read(s, "/Users/james/Library/Application Support/SuperCollider/Extensions/FluidCorpusManipulation/Resources/AudioFiles/Nicol-LoopE-M.wav"); s.sync;
    analyze_to_dataset = {
      arg audio_buffer, slices_buffer, action;
      Routine{
        var features_buf = Buffer(s);
        var stats_buf = Buffer(s);
        var flat_buf = Buffer(s);
        var dataset = FluidDataSet(s);
        var playback_dict = Dictionary.new;
        var playback_dataset;
        playback_dict["cols"] = 2;
        playback_dict["data"] = Dictionary.new;
        slices_buffer.loadToFloatArray(action:{
          arg slices_array;
          slices_array.doAdjacentPairs{
            arg start_frame, end_frame, slice_index;
            var num_frames = end_frame - start_frame;
            "analyzing slice: % / %".format(slice_index + 1,slices_array.size - 1).postln;
            FluidBufChroma.process(s,audio_buffer,start_frame,num_frames,features:features_buf,numChroma:nchroma).wait;
            FluidBufStats.process(s,features_buf,stats:stats_buf).wait;
            FluidBufFlatten.process(s,stats_buf,numFrames:1,destination:flat_buf).wait;
            dataset.addPoint("slice-%".format(slice_index),flat_buf);
            playback_dict["data"]["slice-%".format(slice_index)] = [start_frame, num_frames];
          };
        });
        playback_dataset = FluidDataSet(s).load(playback_dict);
        action.value(dataset, playback_dataset);
      }.play;
    };
  }

  querySynth {
    synth = { |gate=1, query_interval=7, out=0, ampThreshold=(-70.dbamp)|
      var env, gatenv, localbuf, mfccs, playback, playbackbuf, trig,
      start_frame, num_frames, sig, t_kdtree, dur_secs, chroma,
      inenv, input, amp, isPlaying;

      gatenv = EnvGen.kr(Env.asr, gate, doneAction: 0);
      t_kdtree = Impulse.kr(query_interval) * gatenv;
      localbuf = LocalBuf(nmaxchroma, 1);
      playbackbuf = LocalBuf(2, 1);
      input = SoundIn.ar(0);
      amp = Amplitude.kr(input);
      isPlaying = (amp>ampThreshold);
      inenv = EnvGen.ar(Env.asr(0.01,1,1), gate: isPlaying);
      trig = FluidOnsetSlice.ar(input);
      chroma = FluidChroma.kr(input * inenv, numChroma: nchroma);
      FluidKrToBuf.kr(chroma, localbuf);
      kdtree.kr(t_kdtree,localbuf,playbackbuf,1,playback_dataset);
      #start_frame, num_frames = FluidBufToKr.kr(playbackbuf);
      [start_frame, num_frames].postln;
      // dur_secs = num_frames / SampleRate.ir(playbackbuf);
      // env = EnvGen.kr(Env.perc(0.01, dur_secs, 1, -4), trig, doneAction: 0);
      // sig = PlayBuf.ar(1, bufnum: corpus_buf, trigger: trig, startPos: start_frame) * env;
      // Out.ar(out, sig!2);
    }.play;
  }
}