AudioQuery {
  var s, path, nmfccs,
  // Corpus
  <>corpus_buf,<>corpus_indices_buf,
  // Callback functions
  analyze_to_dataset,
  // Datasets
  <>corpus_dataset,<>playback_dataset,
  // Kdtree
  <>kdtree,<>scaled_dataset,<>scaler,
  // Synth
  <>test_sound,<>synth;

  *new { |s,path|
    ^super.newCopyArgs(s ? Server.default, path).init
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
  }

  loadCorpusFilesSync { |corpus_files_folder|
    var loader;
    "loadCorpusFilesSync".postln;
    loader = FluidLoadFolder(corpus_files_folder, channelFunc: nil); s.sync;
    // loader = FluidLoadFolder(corpus_files_folder, channelFunc: {
    //   arg currentFile, channelCount, currentIndex;
    //   fork {
    //     channelCount.postln;
    //     if(channelCount == 1, {[0,0].yield}); s.sync;
    //     if(channelCount == 2, {[0,1].yield}); s.sync;
    //   }
    // }); s.sync;
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

}