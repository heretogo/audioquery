AudioQuery {
  var s, path, nmfccs,
  // Corpus
  <>corpus_buf,<>corpus_indices_buf,
  // Callback functions
  analyze_to_dataset,play_corpus_index,
  // Datasets
  <>scaled_dataset,<>scaler,<>corpus_dataset,<>query_dataset,
  <>playback_dataset,
  // Kdtree
  <>kdtree,
  // Query
  <>query_path,<>query_buf,<>query_indices_buf,
  <>query,<>test,<>test_sound,
  <>synth;

  *new { |s,path|
    ^super.newCopyArgs(s ? Server.default, path ? "/Users/james/Local/Samples/mykit/dr110/").init
  }

  init {
    fork {
      this.setup; s.sync; // initializes buffers, and empty datasets
      this.loadCorpusFilesSync(path); s.sync;
      this.processCorpusSlices; s.sync;
    }
  }

  setup {
    "%: Initializing Class".format(this.class).postln;
    nmfccs = 13;
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
            FluidBufMFCC.process(s,audio_buffer,start_frame,num_frames,features:features_buf,startCoeff:1,numCoeffs:nmfccs).wait;
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

  loadCorpusFilesSync { |corpus_files_folder|
    var loader;
    "loadCorpusFilesSync".postln;
    loader = FluidLoadFolder(corpus_files_folder, channelFunc: {
      arg currentFile, channelCount, currentIndex;
      // channelCount.postln;
      // if(channelCount == 1, {[0,0]});
      // if(channelCount == 2, {[0,1]});
      // An Array of channels to be read from the soundfile. Indices start from zero. These will be read in the order provided.
    }); s.sync;
    loader.play(s,{
      corpus_buf = loader.buffer;
      "all files loaded".postln;
      "num channels: %".format(corpus_buf.numChannels).postln
    }); s.sync;
  }

  processCorpusSlices {
    "processCorpusSlices".postln;
    FluidBufOnsetSlice.process(s,corpus_buf,indices:corpus_indices_buf,metric:9,threshold:0.5,minSliceLength:9,action:{
      corpus_indices_buf.loadToFloatArray(action:{
        arg indices_array;
        "found % slices".format(indices_array.size-1).postln;
        "average length: % seconds".format((corpus_buf.duration / (indices_array.size-1)).round(0.001)).postln;
        this.analyzeCorpusBuffer;
      })
    });
  }

  analyzeCorpusBuffer { // returns dataset
    "analyzeCorpusBuffer".postln;
    analyze_to_dataset.(corpus_buf,corpus_indices_buf, {
      // pass in the audio buffer of the source, and the slice points
      arg ds, pbds;
      corpus_dataset = ds;
      corpus_dataset.print;
      playback_dataset = pbds;
      playback_dataset = pbds;
      this.fitToKDTree;
    });
  }

  analyzeQueryBuffer {
    "analyzeQueryBuffer".postln;
    analyze_to_dataset.(query_buf,query_indices_buf,{
      arg ds;
      query_dataset = ds;
      query_dataset.print;
    });
  }

  fitToKDTree {
    Routine{
      kdtree = FluidKDTree(s);
      scaled_dataset = FluidDataSet(s);
      scaler = FluidNormalize(s); s.sync;
      scaler.fitTransform(corpus_dataset,scaled_dataset,{
        kdtree.fit(scaled_dataset,{
          "kdtree fit".postln;
        });
      });
    }.play;
  }

  querySynth {
    synth = { |gate=1,query_interval=7,out=0|
      var env, localbuf, mfccs, playbackbuf, trig,
          start_frame, num_frames, sig;
      env = EnvGen.kr(Env.asr, gate, doneAction: 0);
      trig = Impulse.kr(query_interval) * env; // send query interval
      localbuf = LocalBuf(nmfccs, 1);
      playbackbuf = LocalBuf(2, 1);
      mfccs = FluidMFCC.kr(PlayBuf.ar(1,test_sound,loop:1),startCoeff:1,numCoeffs:nmfccs);
      // mfccs = FluidMFCC.kr(SoundIn.ar(0),startCoeff:1,numCoeffs:nmfccs);
      FluidKrToBuf.kr(mfccs, localbuf);
      kdtree.kr(trig,localbuf,playbackbuf,1,playback_dataset);
      #start_frame, num_frames = FluidBufToKr.kr(playbackbuf);
      sig = PlayBuf.ar(2, bufnum: corpus_buf, trigger: trig, startPos: start_frame) * env;
      Out.ar(out, sig!2);
    }.play;
  }

}
