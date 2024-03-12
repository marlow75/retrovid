package pl.dido.video.utils;

public class SoundUtils {

    final static float RC = 1f / (2200 * 2 * 3.14f); // 2,2 kHz band
    final static float dt = 1f / 44100;  // 44,1 kHz 
    final static float alpha = dt / (RC + dt);
	
	public static void throwException() throws Exception {
		throw new Exception("DEBUG");
	}

	public final static short simpleLowPass(final short p, final short o) {
		final short delta = (short) (p - o);
		if (delta < 0)
			return (short) (o + (delta <= -6 ? -6 : delta));
		else
			return (short) (o + (delta >= 6 ? 6 : delta));
	}

	public final static float aggresiveLowPass(final float previous, final float current) {
		return 0.85f * previous + 0.15f * (current - previous);
	}
	
	public final static float lowPass(final float previous, final float current) {
	    return (1 - alpha) * previous + alpha * (current - previous);
	}
}