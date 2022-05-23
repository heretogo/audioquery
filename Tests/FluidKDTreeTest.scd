(
    var inputBuffer = Buffer.alloc(s,2);
    var outputBuffer = Buffer.alloc(s,10);//5 neighbours * 2D data points
    s.sync;
    {
        var trig = Impulse.kr(4); //can go as fast as ControlRate.ir/2
        var point = 2.collect{TRand.kr(0,1,trig)};
        point.collect{|p,i| BufWr.kr([p],inputBuffer,i)};
        ~tree.kr(trig,inputBuffer,outputBuffer,5,nil);
        Poll.kr(trig, BufRd.kr(1,outputBuffer,Array.iota(10)),10.collect{|i| "Neighbour" + (i/2).asInteger ++ "-" ++ (i.mod(2))});
        Silent.ar;
    }.play;
}.play;
)