s.boot;

SynthDef(\g1rainer,
	{
		arg trate,d,b,rate=1.2,amp=1.0,gate=0.0,out=0,attack=0.01,sustain=1.0,release=1.0;
		var dur;
		var env = Env.asr(attack,sustain,release);
		var gen = EnvGen.kr(env, Changed.kr(gate));
		dur = (10.rand + 1.0) / (2*trate);
		Out.ar(out,
			TGrains.ar(2,
				Impulse.ar(trate), // trigger
				b, // buffer
				(rate ** WhiteNoise.kr(3).round(1)), // rate
				0,//1.0.rand + d*BufDur.kr(b), //center
				//d*BufDur.kr(b),
				dur, //duration
				WhiteNoise.kr(0.6),//pan
				0.1*amp, //amp
				2
			)*gen;
		);
	}).load;

SynthDef(\mouser,
	{
		arg outx,outy;
		var x,y;
		x = MouseX.kr(0,1,0);
		y = MouseY.kr(0,1,0);
		Out.kr(outx,x);
		Out.kr(outy,y);
	}).load;



// Hilbert stuff

~rot = {|n,x,y,rx,ry|
	var t;
    if(ry == 0,{
        if(rx == 1, {
            x = (n-1) - x;
            y = (n-1) - y;
        });
		t = y;
		y = x;
		x = t;
    });
	[x,y];
};

~xy2d = {|n,x,y| 
	var rx = 0, ry = 0, s = 0, d =0,v;
	n = n.asInteger;
	s = n.div(2).asInteger;
	x = x.asInteger;
	y = y.asInteger;
	while({s > 0},
		{
			rx = ((x & s) > 0).asInteger;
			ry = ((y & s) > 0).asInteger;
			d = (d + (s * s * ((3 * rx).asInteger().bitXor(ry)))).asInteger;
			//[s,rx,ry,d].postln;		
			v = ~rot.(s, x, y, rx, ry);
			x = v[0].asInteger;
			y = v[1].asInteger;
			s = s.div(2).asInteger;
		});
	d;
};
~hilbert = {arg x,y,n=64;
	~xy2d.(n,x*(n-1),y*(n-1))/(n*n - 1)
};

// Boot the server

// Load an audio file
~buffers = [
"/home/hindle1/projects/arts-birthday-2015/goldberg-aria-da-capo.wav",
"/home/hindle1/projects/arts-birthday-2015/peergynt1.wav",
"/home/hindle1/projects/arts-birthday-2015/Gesualdo-O_vos_omnes-stretch.wav",
"/home/hindle1/projects/arts-birthday-2015/peergynt5.wav",
"/home/hindle1/projects/arts-birthday-2015/india-bird-reverb-clean-old2.wav",
"/home/hindle1/projects/arts-birthday-2015/peergynt2.wav",
"/home/hindle1/projects/arts-birthday-2015/peergynt3.wav",
"/home/hindle1/projects/arts-birthday-2015/peergynt4.wav",
"/home/hindle1/projects/arts-birthday-2015/india-bird-reverb-clean.wav",
"/home/hindle1/projects/arts-birthday-2015/peergynt5-old1.wav",
	
	"./wavs/ma-vlast.wav",
	"./wavs/linesrender.wav",
	"./wavs/hurricane.wav",
	"./wavs/annie.wav",
	"./wavs/fmout.wav",
	"./wavs/sweep.wav",
	"./wavs/1.harmonica.wav"].collect({|x| Buffer.read(s,x) });



~buff  = ~buffers[0 % ~buffers.size];
~buff2 = ~buffers[1 % ~buffers.size];
~buff3 = ~buffers[2 % ~buffers.size];
~buff4 = ~buffers[3 % ~buffers.size];

b = ~buff.bufnum;
~b2 =~buff2.bufnum;
~b3 =~buff3.bufnum;
~b4 =~buff4.bufnum;





// play the synth
x = Synth(\g1rainer,[\rate,2,\d,0.5,\b,b]);
x.set(\d,0.2);
x.set(\b,b);
~xd = Bus.control(s);
x.map(\d,~xd);
~xd.set(0.1);

y = Synth(\g1rainer,[\trate,2,\d,0.5,\b,~buff4]);
y.map(\d,~xd);
z = Synth(\g1rainer,[\trate,2,\d,0.5,\b,~buff3]);
z.map(\d,~xd);
u = Synth(\g1rainer,[\trate,2,\d,0.5,\b,~buff2]);
u.map(\d,~xd);
u.set(\rate,5.0);
u.set(\trate,33);
x.set(\amp,1.0);
y.set(\amp,1.0);
y.set(\rate,1.0);
x.set(\rate,1.0);
y.set(\trate,3);
x.set(\trate,3);

x.set(\amp,1.0);
y.set(\amp,1.0);
z.set(\amp,1.0);
u.set(\amp,1.0);
// xy coords and buses for x and y coords
~xm = 0;
~ym = 0;
~xmb = Bus.control(s);
~ymb = Bus.control(s);


// Get the mouse outputs

~mouser = Synth(\mouser,[\outx,~xmb,\outy,~ymb]);
~mrot = Routine({
	var lastxm = 0, lastym = 0;
	while(true,{
		~xmb.get({|v| ~xm = v; });
		~ymb.get({|v| ~ym = v; });
		if(~xm == lastxm && ~ym == lastym,{
			
		},{
			~d.postln;
			//~amp.set(1.0);
			~d = ~hilbert.(~xm,~ym,n=1024);
			~xd.set(~d);
			u.set(\gate,1.0.rand + 0.1);
			x.set(\gate,1.0.rand + 0.1);
			y.set(\gate,1.0.rand + 0.1);
			z.set(\gate,1.0.rand + 0.1);
		});
		lastxm = ~xm;
		lastym = ~ym;
		0.05.wait;
	});
}).play;
//~mrot.stop;

~mkgranwrap = {|syn|
	{
		|msg|
		var xx = msg[1], yy = msg[2], dd;
		dd = ~hilbert.(xx,yy,n=256);
		dd.postln;
		syn.set(\d,dd);
		syn.set(\amp,1.0);
		syn.set(\gate,1.0.rand + 0.1);
		//~amp.set(1.0);
	};
};

OSCFunc.newMatching(~mkgranwrap.(x),'/multi/1');
OSCFunc.newMatching(~mkgranwrap.(y),'/multi/2');
OSCFunc.newMatching(~mkgranwrap.(z),'/multi/3');
OSCFunc.newMatching(~mkgranwrap.(u),'/multi/4');

/*
(
{
    var env = Env.asr(0.01,1.0,1.0);
    var gate = MouseX.kr(-1.0, 3);
    var gen = EnvGen.kr(env, gate);
    SinOsc.ar(270, SinOsc.ar(gen * 473)) * gen * 0.2
}.play
)

Env.new([0, 1, 0.9, 0], [0.1, 0.5, 1],[-5, 0, -5]).plot;
*/


~randr = {|v|
	v.set(\rate,0.2.rand + 0.9);//1.001);//0.4.rand + 1.0);
	v.set(\trate,1.0.rand + 0.2);//1.9.rand + 0.4);	
	v.set(\b,~buffers.choose.bufnum);
};
~randr.(x);
~randr.(y);
~randr.(z);
~randr.(u);
