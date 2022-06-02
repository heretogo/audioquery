~input = { SoundIn.ar(0) }.play;
~chroma = AudioQueryChroma(s,"/Users/james/Local/Samples/mykit/mono/");
~chroma.querySynth;
~chroma.synth.free;

~chroma.corpus_dataset.print;
-> FluidDataSet(41)
DataSet 41:
rows: 13 cols: 2
slice-0          0      9216
slice-1       9216     16384
slice-10 2.4627e+05     19456
       ...
slice-7 1.6077e+05     49152
slice-8 2.0992e+05     16384
slice-9  2.263e+05     19968

~chroma.playback_dataset.print
-> FluidDataSet(1)
DataSet 1:
rows: 13 cols: 12
slice-0   0.022503  0.019419  0.018553       ...  0.043007  0.040106  0.031424
slice-1  0.0095678 0.0075858 0.0086798       ...  0.075579  0.042301  0.021146
slice-2   0.069244  0.051654  0.031619       ...  0.050084  0.059084    0.0683
       ...
slice-10 8.7381e-050.000148140.00014491       ... 0.0030428 0.0034609  0.000456
slice-11   0.013177  0.020858  0.018256       ... 0.0010328 0.0012196 0.0061449
slice-12  0.0014986 0.0022772 0.0075069       ... 0.0012157 0.0010677 0.0011916

// FluidBufChroma
// FluidChroma