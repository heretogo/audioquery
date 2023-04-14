~flucoma[\audioQueryDR110] = AudioQueryMFCC(s,"/Users/james/Local/Sources/Audio/Samples/mykit/mono/");
~flucoma[\audioQueryTrek].querySynth;

~flucoma[\audioQueryTrek] = AudioQueryMFCC(s,"/Users/james/Local/Sources/Audio/Samples/trek/all/");
~flucoma[\audioQueryTrek].querySynth;

~flucoma[\audioQueryBongo] = AudioQueryMFCC(s,"/Users/james/Local/Sources/Audio/Samples/mykit/bongo/");
~flucoma[\audioQueryTimpani] = AudioQueryMFCC(s,"/Users/james/Local/Sources/Audio/Samples/mykit/timpani/");
~flucoma[\audioQueryTabla] = AudioQueryMFCC(s,"/Users/james/Local/Sources/Audio/Samples/tabla/");

~input = { SoundIn.ar(0) }.play;
~input.free;
~query.kdtree;
~query.querySynth;
~query.synth.free;
~query.synth.set(\gate, 1);
~query.synth.set(\query_interval,200);
~query.synth.set(\ampThreshold,-70.dbamp);
~query.corpus_buf.play;

~query.playback_dataset.print

FluidLoadFolder