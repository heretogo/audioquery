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
  <>query,<>test,
  <>synth;

  *new { |s,path|
    ^super.newCopyArgs(s ? Server.default, path ? "/Users/james/Local/Samples/mykit/dr110/").init
  }

  init {
    fork {
      this.setup; s.sync;
      this.loadCorpusFilesSync(path); s.sync;
      this.processCorpusSlices; s.sync;
      this.loadQueryBufferSync; s.sync;
      this.processQuerySlices; s.sync;
    }
  }

  setup {
    "%: Initializing Class".format(this.class).postln;
    nmfccs = 13;
    corpus_indices_buf = Buffer(s); s.sync;

    analyze_to_dataset = {
      arg audio_buffer, slices_buffer, action;
      Routine{
        var features_buf = Buffer(s);
        var stats_buf = Buffer(s);
        var flat_buf = Buffer(s);
        var dataset = FluidDataSet(s);
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
            // TODO: HERE -->>
            //         playdataset.addPoint
            //         setn(startframe, numframes)
            // FluidDataSet // starting sample, num samples (, index)
          };
        });
        action.value(dataset);
      }.play;
    };

    play_corpus_index = {
      arg index, src_dur;
      {
        var start_frame = Index.kr(corpus_indices_buf,index); // lookup the start frame with the index *one the server* using Index.kr
        var end_frame = Index.kr(corpus_indices_buf,index+1); // same for the end frame
        var num_frames = end_frame - start_frame;
        var dur_secs = min(num_frames / SampleRate.ir(corpus_buf),src_dur);
        var sig = PlayBuf.ar(1,corpus_buf,BufRateScale.ir(corpus_buf),0,start_frame,0,2);
        var env = EnvGen.kr(Env([0,1,1,0],[0.03,dur_secs-0.06,0.03]),doneAction:2);
        sig = sig * env;
        sig.dup;
      }.play;
    };

  }

  loadCorpusFilesSync { |corpus_files_folder|
    var loader;
    "loadCorpusFilesSync".postln;
    loader = FluidLoadFolder(corpus_files_folder, channelFunc: {
      arg currentFile, channelCount, currentIndex;
      if(channelCount == 1, {^[0,0]});
      if(channelCount == 2, {^[0,1]});
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
      arg ds;
      corpus_dataset = ds;
      corpus_dataset.print;
      this.fitToKDTree;
    });
  }

  loadQueryBufferSync {
    "readQueryBufferSync".postln;
    query_path = "/Users/james/Library/Application Support/SuperCollider/Extensions/FluidCorpusManipulation/Resources/AudioFiles/Nicol-LoopE-M.wav";
    query_buf = Buffer.read(s,query_path); s.sync;
  }

  processQuerySlices {
    "processQuerySlices".postln;
    query_indices_buf =  Buffer(s);
    FluidBufOnsetSlice.process(s,query_buf,indices:query_indices_buf,metric:9,threshold:0.5,action:{
      query_indices_buf.loadToFloatArray(action:{
        arg indices_array;
        // post the results so that you can tweak the parameters and get what you want
        "found % slices".format(indices_array.size-1).postln;
        "average length: % seconds".format((query_buf.duration / (indices_array.size-1)).round(0.001)).postln;
        this.analyzeQueryBuffer;
      })
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

  testDrumLoop {
    test = Routine{
      query_indices_buf.loadToFloatArray(action:{
        arg query_indices_array;
        query_indices_array = [0] ++ query_indices_array;
        query_indices_array = query_indices_array ++ [query_buf.numFrames];

        inf.do{ // loop for infinity
                // TODO: make pausable
          arg i;
          var index = i % (query_indices_array.size - 1);
          var slice_id = index - 1;
          var start_frame = query_indices_array[index];
          var dur_frames = query_indices_array[index + 1] - start_frame;
          var dur_secs = dur_frames / query_buf.sampleRate;

          "playing slice: %".format(slice_id).postln;

          {
            var sig = PlayBuf.ar(1,query_buf,BufRateScale.ir(query_buf),0,start_frame,0,2);
            var env = EnvGen.kr(Env([0,1,1,0],[0.03,dur_secs-0.06,0.03]),doneAction:2);
            sig = sig * env;
            sig.dup;
          }.play;
          dur_secs.wait;
        };
      });
    }.play;
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
    synth = {
      var mfccs, trig;
      trig = Impulse.kr(4);
      mfccs = FluidMFCC.kr(SoundIn.ar(0),startCoeff:1,numCoeffs:nmfccs);
      // FluidKrToBuf.kr(mfccs, query_buf);
      kdtree.kr(trig,corpus_buf,query_buf,5,nil);
      Poll.kr(trig, BufRd.kr(1,query_buf,Array.iota(10)),10.collect{|i| "Neighbour" + (i/2).asInteger ++ "-" ++ (i.mod(2))});
      // sig = PlayBuf.ar(index, corpus_buf, trig);
      // Out.ar(out, sig);
    }.play;
  }

  querySounds {
    query = Routine{
      var query_buf = Buffer.alloc(s,nmfccs); // a buffer for doing the neighbor lookup with
      var scaled_buf = Buffer.alloc(s,nmfccs);
      query_indices_buf.loadToFloatArray(action:{
        arg query_indices_array;

        // prepend 0 (the start  of the file) to the indices array
        query_indices_array = [0] ++ query_indices_array;

        //  append the total number of frames to know how long to play the last slice for
        query_indices_array = query_indices_array ++ [query_buf.numFrames];

        inf.do{ // loop for infinity
          arg i;

          // get the index to play by modulo one less than the number of slices (we don't want to *start* playing from the
          // last slice point, because that's the end of the file!)
          var index = i % (query_indices_array.size - 1);

          // nb. that the minus one is so that the drum slice from the beginning of the file to the first index is call "-1"
          // this is because that slice didn't actually get analyzed
          var slice_id = index - 1;
          var start_frame = query_indices_array[index];
          var dur_frames = query_indices_array[index + 1] - start_frame;

          // this will be used to space out the source slices according to the target timings
          var dur_secs = dur_frames / query_buf.sampleRate;

          "target slice: %".format(slice_id).postln;

          // as long as this slice is not the one that starts at the beginning of the file (-1) and
          // not the slice at the end of the file (because neither of these have analyses), let's
          // do the lookup
          if((slice_id >= 0) && (slice_id < (query_indices_array.size - 3)),{

            // use the slice id to (re)create the slice identifier and load the data point into "query_buf"
            query_dataset.getPoint("slice-%".format(slice_id.asInteger),query_buf,{
              // once it's loaded, scale it using the scaler
              scaler.transformPoint(query_buf,scaled_buf,{
                // once it's neighbour data point in the kdtree of source slices
                kdtree.kNearest(scaled_buf,{
                  arg nearest;

                  // peel off just the integer part of the slice to use in the helper function
                  var nearest_index = nearest.asString.split($-)[1].asInteger;
                  nearest_index.postln;
                  play_corpus_index.(nearest_index,dur_secs);
                });
              });
            });
          });

          dur_secs.wait;
        };
      });
    }.play;
  }

}
