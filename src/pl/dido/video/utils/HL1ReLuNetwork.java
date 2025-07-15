package pl.dido.video.utils;

public class HL1ReLuNetwork extends HL1Network {
	
	private static final long serialVersionUID = 1L;
	protected float leakiness = 0f;
			
	protected float activation(final float x) {
		return x < 0f ? x * leakiness : x;
	}
	
	protected float derivative(final float x) {
		return x < 0 ? leakiness : 1f;
	}
	
	public HL1ReLuNetwork(final int in, final int hid, final int out) {
		super(in, hid, out);
	}
}