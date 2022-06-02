~query = AudioQueryMFCC(s,"/Users/james/Local/Samples/mykit/mono/");
~query = AudioQueryMFCC(s,"/Users/james/Local/Samples/mykit/bongo/");
~query = AudioQueryMFCC(s,"/Users/james/Local/Samples/mykit/timpani/");
~query = AudioQueryMFCC(s,"/Users/james/Local/Samples/tabla/");
~query = AudioQueryMFCC(s,"/Users/james/Local/Samples/trek/all/");

~input = { SoundIn.ar(0) }.play;
~query.kdtree;
~query.querySynth;
~query.synth.free;
~query.synth.set(\gate, 0);
~query.synth.set(\query_interval,200);
~query.synth.set(\ampThreshold,-70.dbamp);
~query.corpus_buf.play;

FluidLoadFolder