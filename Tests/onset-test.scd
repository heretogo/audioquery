~query = AudioQuery(s,"/Users/james/Local/Samples/mykit/mono/");
~query = AudioQuery(s);
// ERROR: FluidNormalize - Wrong Point Size

~query.source_dataset.print
~query.target_dataset.print
~query.fitToKDTree;
~query.kdtree;

~query.testDrumLoop;
~query.test.stop;
~query.test.reset;
~query.test.play;

~query.querySounds;
~query.query.stop;

FluidLoadFolder
kdTree.kr

SoundFile

Buffer