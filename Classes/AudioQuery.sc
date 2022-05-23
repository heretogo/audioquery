
AudioQuery {
  var s, path, nmfccs,
  // Source
  <>source_buf,<>source_indices_buf,
  // Callback functions
  analyze_to_dataset,play_source_index,
  // Datasets
  <>scaled_dataset,<>scaler,<>source_dataset,<>target_dataset,
  // Kdtree
  <>kdtree,
  // Target
  <>target_path,<>target_buf,<>target_indices_buf,
  <>query,<>test,
  <>synth;

  *new { |s,path|
    ^super.newCopyArgs(s ? Server.default, path ? "/Users/james/Local/Samples/mykit/dr110/").init
  }

  init {
    fork {
      this.setup; s.sync;
      this.loadSourceFilesSync(path); s.sync;
      this.processSourceSlices; s.sync;
      this.loadTargetBufferSync; s.sync;
      this.processTargetSlices; s.sync;
    }
  }

  setup {
    "%: Initializing Class".format(this.class).postln;
    nmfccs = 13;
    source_indices_buf = Buffer(s); s.sync;

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
            // playdataset.addPoint
            // setn(startframe, numframes)
          };
        });
        action.value(dataset);
      }.play;
    };

    play_source_index = {
      arg index, src_dur;
      {
        var start_frame = Index.kr(source_indices_buf,index); // lookup the start frame with the index *one the server* using Index.kr
        var end_frame = Index.kr(source_indices_buf,index+1); // same for the end frame
        var num_frames = end_frame - start_frame;
        var dur_secs = min(num_frames / SampleRate.ir(source_buf),src_dur);
        var sig = PlayBuf.ar(1,source_buf,BufRateScale.ir(source_buf),0,start_frame,0,2);
        var env = EnvGen.kr(Env([0,1,1,0],[0.03,dur_secs-0.06,0.03]),doneAction:2);
        // sig = sig * env; // include this env if you like, but keep the line above because it will free the synth after the slice!
        sig.dup;
      }.play;
    };

  }

  loadSourceFilesSync { |source_files_folder|
    var loader;
    "loadSourceFilesSync".postln;
    loader = FluidLoadFolder(source_files_folder, channelFunc: {
      // arg currentFile, channelCount, currentIndex;
      // [currentFile, channelCount, currentIndex].postln;
      // if mono:
      //  [0,0]
      // if stereo:
      //  [0,1]
      // buffer.readChannel(f.path,bufStartFrame:startEnd[i][0], channels:channelMap);
    }); s.sync;
    loader.play(s,{
      source_buf = loader.buffer;
      "all files loaded".postln;
      "num channels: %".format(source_buf.numChannels).postln
    }); s.sync;
  }

  processSourceSlices {
    "processSourceSlices".postln;
    FluidBufOnsetSlice.process(s,source_buf,indices:source_indices_buf,metric:9,threshold:0.5,minSliceLength:9,action:{
      source_indices_buf.loadToFloatArray(action:{
        arg indices_array;
        "found % slices".format(indices_array.size-1).postln;
        "average length: % seconds".format((source_buf.duration / (indices_array.size-1)).round(0.001)).postln;
        this.analyzeSourceBuffer;
      })
    });
  }

  analyzeSourceBuffer { // returns dataset
    "analyzeSourceBuffer".postln;
    analyze_to_dataset.(source_buf,source_indices_buf, {
      // pass in the audio buffer of the source, and the slice points
      arg ds;
      source_dataset = ds;
      source_dataset.print;
      this.fitToKDTree;
    });
  }

  loadTargetBufferSync {
    "readTargetBufferSync".postln;
    target_path = "/Users/james/Library/Application Support/SuperCollider/Extensions/FluidCorpusManipulation/Resources/AudioFiles/Nicol-LoopE-M.wav";
    target_buf = Buffer.read(s,target_path); s.sync;
  }

  processTargetSlices {
    "processTargetSlices".postln;
    target_indices_buf =  Buffer(s);
    FluidBufOnsetSlice.process(s,target_buf,indices:target_indices_buf,metric:9,threshold:0.5,action:{
      target_indices_buf.loadToFloatArray(action:{
        arg indices_array;
        // post the results so that you can tweak the parameters and get what you want
        "found % slices".format(indices_array.size-1).postln;
        "average length: % seconds".format((target_buf.duration / (indices_array.size-1)).round(0.001)).postln;
        this.analyzeTargetBuffer;
      })
    });
  }

  analyzeTargetBuffer {
    "analyzeTargetBuffer".postln;
    analyze_to_dataset.(target_buf,target_indices_buf,{
      arg ds;
      target_dataset = ds;
      target_dataset.print;
    });
  }

  testDrumLoop {
    test = Routine{
      target_indices_buf.loadToFloatArray(action:{
        arg target_indices_array;
        target_indices_array = [0] ++ target_indices_array;
        target_indices_array = target_indices_array ++ [target_buf.numFrames];

        inf.do{ // loop for infinity
                // TODO: make pausable
          arg i;
          var index = i % (target_indices_array.size - 1);
          var slice_id = index - 1;
          var start_frame = target_indices_array[index];
          var dur_frames = target_indices_array[index + 1] - start_frame;
          var dur_secs = dur_frames / target_buf.sampleRate;

          "playing slice: %".format(slice_id).postln;

          {
            var sig = PlayBuf.ar(1,target_buf,BufRateScale.ir(target_buf),0,start_frame,0,2);
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
      scaler.fitTransform(source_dataset,scaled_dataset,{
        kdtree.fit(scaled_dataset,{
          "kdtree fit".postln;
        });
      });
    }.play;
  }

  querySynth {
    synth = {
      var trig = Impulse.kr(4); // paramaterize or use onset detector
      var point = 2.collect{TRand.kr(0,1,trig)};
      point.collect{|p,i| BufWr.kr([p],source_buf)};
      // kdtree.kr(trig,source_buf,target_buf,5,nil);
      // mfccs = FluidMFCC.kr(SoundIn.ar(0)):
      // FluidKrToBuf.kr(mfccs, target_buf);
      // params must match
      kdtree.kr(trig,target_buf,5,nil);
      // Poll.kr(trig, BufRd.kr(1,target_buf,Array.iota(10)),10.collect{|i| "Neighbour" + (i/2).asInteger ++ "-" ++ (i.mod(2))});
      // sig = PlayBuf.ar(index, buf, trig);
      sig = PlayBuf.ar(, source_buf, trig);
      // Out.ar(out, sig);
    }.play;
  }

  querySounds {
    query = Routine{
      var query_buf = Buffer.alloc(s,nmfccs); // a buffer for doing the neighbor lookup with
      var scaled_buf = Buffer.alloc(s,nmfccs);
      target_indices_buf.loadToFloatArray(action:{
        arg target_indices_array;

        // prepend 0 (the start  of the file) to the indices array
        target_indices_array = [0] ++ target_indices_array;

        //  append the total number of frames to know how long to play the last slice for
        target_indices_array = target_indices_array ++ [target_buf.numFrames];

        inf.do{ // loop for infinity
          arg i;

          // get the index to play by modulo one less than the number of slices (we don't want to *start* playing from the
          // last slice point, because that's the end of the file!)
          var index = i % (target_indices_array.size - 1);

          // nb. that the minus one is so that the drum slice from the beginning of the file to the first index is call "-1"
          // this is because that slice didn't actually get analyzed
          var slice_id = index - 1;
          var start_frame = target_indices_array[index];
          var dur_frames = target_indices_array[index + 1] - start_frame;

          // this will be used to space out the source slices according to the target timings
          var dur_secs = dur_frames / target_buf.sampleRate;

          "target slice: %".format(slice_id).postln;

          // as long as this slice is not the one that starts at the beginning of the file (-1) and
          // not the slice at the end of the file (because neither of these have analyses), let's
          // do the lookup
          if((slice_id >= 0) && (slice_id < (target_indices_array.size - 3)),{

            // use the slice id to (re)create the slice identifier and load the data point into "query_buf"
            target_dataset.getPoint("slice-%".format(slice_id.asInteger),query_buf,{
              // once it's loaded, scale it using the scaler
              scaler.transformPoint(query_buf,scaled_buf,{
                // once it's neighbour data point in the kdtree of source slices
                kdtree.kNearest(scaled_buf,{
                  arg nearest;

                  // peel off just the integer part of the slice to use in the helper function
                  var nearest_index = nearest.asString.split($-)[1].asInteger;
                  nearest_index.postln;
                  play_source_index.(nearest_index,dur_secs);
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
