~input = { SoundIn.ar(0) }.play;
~chroma = AudioQueryChroma(s,"/Users/james/Local/Samples/mykit/mono/");
~chroma.querySynth;
~chroma.synth.free;

~input = { SoundIn.ar(0) }.play;
~chroma = AudioQueryChroma(s,"/Users/james/Local/Samples/corpus/chroma/");
~chroma.querySynth;
~chroma.synth.free;
~chroma.corpus_buf.play

~chroma.corpus_dataset.print
~chroma.playback_dataset.print

// FluidBufChroma
// FluidChroma

FluidBufOnsetSlice