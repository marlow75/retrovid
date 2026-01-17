package pl.dido.video.utils;

public class SoundUtils {

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

	public final static int lowPass(final int previous, final int current) {
		return Math.round(previous + 0.5f * (current - previous));
	}

	public final static float triangularDistribution(final float a, final float c, final float b) {
		final float u = (float) Math.random();
		final float fc = (c - a) / (b - a);

		if (u < fc)
			return a + (float) Math.sqrt(u * (b - a) * (c - a));
		else
			return b - (float) Math.sqrt((1 - u) * (b - a) * (b - c));
	}

	public static short[] compressAndNormalize(final short[] input) {
		final float preGainDb = 0f; // przed kompresorem (dB)
		final float thresholdDb = -6f;

		final float ratio = 1.5f;
		final float attackMs = 10f;

		final float releaseMs = 200f;
		final float makeupDb = 3f;

		final int sampleRate = 44100;
		final int channels = 1;

		final float detectorWindowMs = 50f; // dla RMS detektora (równowa¿ne tau)
		final float targetPeak = 0.95f; // fraction of full scale (1.0 -> 32767)

		final int frames = input.length / channels;
		final float[] out = new float[input.length]; // floats for internal processing

		// 1) przygotuj parametry detektora i wyg³adzania gainu
		final float preGain = (float) Math.pow(10.0, preGainDb / 20.0);
		final float makeupGain = (float) Math.pow(10.0, makeupDb / 20.0);

		// RMS detektor: wyg³adzanie wyk³adnicze dla x^2
		final float detectorTau = Math.max(1f, detectorWindowMs) / 1000f; // s
		final float detectorAlpha = (float) Math.exp(-1.0 / (detectorTau * sampleRate));

		final float attackTime = Math.max(0.5f, attackMs) / 1000f;
		final float releaseTime = Math.max(1f, releaseMs) / 1000f;

		final float attackCoeff = (float) Math.exp(-1.0 / (attackTime * sampleRate));
		final float releaseCoeff = (float) Math.exp(-1.0 / (releaseTime * sampleRate));

		float detectorEnv = 0f; // env of squared signal
		float smoothGain = 1f; // linear gain applied to signal (smoothed)

		final float eps = 1e-9f;

		// 1) pierwsze przejœcie: kompresja (per sample)
		for (int i = 0; i < frames; i++) {
			// oblicz mono level do detekcji (u¿ywamy RMS po soucie kana³ów)
			float sumSq = 0f;

			for (int c = 0; c < channels; c++) {
				float s = (float) input[i * channels + c];

				// normalizacja do -1..1
				s = s / 32768f;
				// preGain
				s *= preGain;
				sumSq += s * s;
			}

			final float rmsSq = sumSq / channels;

			// EMA na rms^2
			detectorEnv = detectorAlpha * detectorEnv + (1f - detectorAlpha) * rmsSq;
			final float rms = (float) Math.sqrt(Math.max(detectorEnv, 0f));

			// poziom w dB (RMS)
			final float levelDb = 20f * (float) Math.log10(Math.max(rms, eps));

			// oblicz gain reduction w dB
			float gainDb = 0f;
			if (levelDb > thresholdDb) {

				float above = levelDb - thresholdDb;
				float reducedAbove = above / ratio;

				float compDb = thresholdDb + reducedAbove;
				gainDb = compDb - levelDb; // <= 0
			}

			final float desiredGain = (float) Math.pow(10.0, gainDb / 20.0); // <= 1

			if (desiredGain < smoothGain)
				smoothGain = attackCoeff * smoothGain + (1f - attackCoeff) * desiredGain;
			else
				smoothGain = releaseCoeff * smoothGain + (1f - releaseCoeff) * desiredGain;

			// final gain = smoothGain * makeupGain, zastosuj do ka¿dego kana³u
			final float totalGain = smoothGain * makeupGain;
			for (int c = 0; c < channels; c++) {
				float s = (float) input[i * channels + c];

				s = s / 32768f;
				s *= preGain;
				s *= totalGain;

				// soft limiting przy overflow (opcjonalne, proste)
				if (s > 1f)
					s = 1f;
				else 
				if (s < -1f)
					s = -1f;

				out[i * channels + c] = s;
			}
		}

		// 4) finalna peak-normalizacja do targetPeak
		float maxAbs = 0f;
		for (int i = 0; i < out.length; i++) {
			
			final float a = Math.abs(out[i]);
			if (a > maxAbs)
				maxAbs = a;
		}

		if (maxAbs < eps)
			maxAbs = eps;

		final float finalTarget = Math.max(0.0f, Math.min(1.0f, targetPeak)); // np. 0.98
		final float finalGain = finalTarget / maxAbs;

		final short[] result = new short[out.length];
		for (int i = 0; i < out.length; i++) {
			float v = out[i] * finalGain;

			// clip to short range
			int iv = Math.round(v * 32767f);

			if (iv > 32767)
				iv = 32767;
			else 
			if (iv < -32768)
				iv = -32768;

			result[i] = (short) iv;
		}

		return result;
	}
}