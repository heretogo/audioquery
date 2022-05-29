~query = AudioQuery(s,"/Users/james/Local/Samples/mykit/mono/");
~query.querySynth; // default query_interval is 4
~query.synth.free;
~query.synth.set(\query_interval,7);
~query.corpus_buf.play;

~query2 = AudioQuery(s,"/Users/james/Local/Samples/mykit/bongo/");
~query2.querySynth;
~query2.synth.set(\gate, 0);
~query2.synth.set(\query_interval,0.5);
~query2.corpus_buf.play;

~query3 = AudioQuery(s,"/Users/james/Local/Samples/mykit/all/");
~query3.querySynth;
~query3.synth.set(\gate, 0);
~query3.synth.set(\query_interval,0.5);
~query3.corpus_buf.play;

~query4 = AudioQuery(s,"/Users/james/Local/Samples/trek/all/");
~query4.test_sound.play;
~query4.querySynth; // \gate, 1, \query_interval, 7
~query4.synth.set(\gate, 0);
~query4.synth.set(\query_interval, 7);