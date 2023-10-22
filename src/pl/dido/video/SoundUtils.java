package pl.dido.video;

public class SoundUtils {

    final static float RC = 1f / (1800 * 2 * 3.14f); // 1,8 kHz band
    final static float dt = 1f / 4400;  // 4400 kHz 
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

	public final static float lowPassAverage(final float previous, final float current) {
		return 0.1f * current + 0.9f * previous;
	}

	public final static float aggresivelowPass(final float previous, final float current) {
		final float a = 0.15f;
		
		return previous + a * (current - previous);
	}
	
	public final static float lowPass(final float previous, final float current) {
	    return previous + alpha * (current - previous);
	}
}