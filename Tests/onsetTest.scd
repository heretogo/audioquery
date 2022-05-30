~query = AudioQuery(s,"/Users/james/Local/Samples/mykit/mono/");
~query = AudioQuery(s,"/Users/james/Local/Samples/mykit/bongo/");
~query = AudioQuery(s,"/Users/james/Local/Samples/trek/all/");
// ~query = AudioQuery(s,"/Users/james/Local/Samples/tabla/");

~query.querySynth; // default query_interval is 4
~query.synth.free;
~query.synth.set(\gate, 0);
~query.synth.set(\query_interval,40);
~query.synth.set(\ampThreshold,-70.dbamp);
~query.corpus_buf;

Playbuf
FluidBufOnsetSlice
FluidLoadFolder