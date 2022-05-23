~query = AudioQuery(s,"/Users/james/Local/Samples/mykit/mono/");
// ERROR: FluidNormalize - Wrong Point Size

~query.source_dataset.print
~query.target_dataset.print
~query.fitToKDTree;
~query.kdtree.kr

kdtree.kr(trig,target_buf,5,dataset);

~query.testDrumLoop;
~query.test.stop;
~query.test.reset;
~query.test.play;

~query.querySounds;
~query.query.stop;

~query.querySynth;
~query.synth.free;

Buffer

FluidMFCC

FluidKDTree

///

FluidDataSet
starting sample, num samples (, index)
PlayBuf.ar;