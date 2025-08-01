package pl.dido.video.utils;

public class SoundUtils {

	final static float RC = 1f / (2756 * 2 * 3.14f); // cutoff @ 2,7 kHz (2756)
	final static float dt = 1f / 44100; // 44,1 kHz
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

	public final static int lowPass(final int previous, final int current) {
		return Math.round((1 - alpha) * previous + alpha * (current - previous));
	}

	public final static float triangularDistribution(final float a, final float c, final float b) {
		final float u = (float) Math.random();
		final float fc = (c - a) / (b - a);

		if (u < fc)
			return a + (float) Math.sqrt(u * (b - a) * (c - a));
		else
			return b - (float) Math.sqrt((1 - u) * (b - a) * (b - c));
	}

	public static void normalize2dBFS(final short samples[], final float dBFS) {
		int maxSample = 0;

		for (final int sample : samples) {
			final int abs = Math.abs(sample);
			if (abs > maxSample)
				maxSample = abs;
		}

		if (maxSample == 0)
			return;

		final float targetLevel = (float) (maxSample * Math.pow(10f, dBFS / 20f));
		final float normalizationFactor = targetLevel / maxSample;

		for (int i = 0; i < samples.length; i++)
			samples[i] = (byte) Math.round(samples[i] * normalizationFactor);
	}

	public static void peekNormalization(final short samples[]) {
		int maxSample = 0, minSample = 32768;

		for (final int sample : samples) {
			final int abs = Math.abs(sample);
			
			if (abs > maxSample)
				maxSample = abs;
			
			if (abs < minSample)
				minSample = abs;
		}

		if (maxSample == 0)
			return;
		
		for (int i = 0; i < samples.length; i++) {
			final int data = samples[i];
			final int abs = Math.abs(data);
			
			samples[i] = (short) (Math.signum(data) * ((abs * (maxSample - minSample)) / 32768 + maxSample));
		}
	}
}