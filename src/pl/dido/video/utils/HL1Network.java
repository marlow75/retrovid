package pl.dido.video.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import at.fhtw.ai.nn.utils.Dataset;
import at.fhtw.ai.nn.utils.Network;
import at.fhtw.ai.nn.utils.NetworkProgressListener;

public class HL1Network implements Network, Serializable {
	private static final long serialVersionUID = 1L;
	private ArrayList<NetworkProgressListener> listeners;
	
	public static Logger log = Logger.getLogger(HL1Network.class.getCanonicalName());

	public int IN  = 8 * 8; // input layer
	public int HID = 2 * 8 * 8; // hidden layer
	
	public int OUT = 256; // out
	public float ERR_LIMIT = 0.05f;
	
	// adding momentum
	public float MOMENTUM   = 0.9f;
	public float LEARN_RATE = 0.02f;

	// max epochs
	public float EPOCHS = 1_000;
	public float[] I;

	public float[] H;
	public float[] O;

	public float[][] W;
	public float[][] V;

	public float[][] dW;
	public float[][] dV;

	public float[] hBias;
	public float[] oBias;
	
	public float[] hsBias;
	public float[] osBias;

	public float[] eOutput;
	public float[] eHidden;
	
	protected Random rnd;

	public HL1Network(final int in, final int hid, final int out) {
		IN = in; HID = hid; OUT = out;
		
		I = new float[IN];

		H = new float[HID];
		O = new float[OUT];
		
		W = new float[HID][IN];
		V = new float[OUT][HID];

		dW = new float[HID][IN];
		dV = new float[OUT][HID];

		hBias  = new float[HID];
		hsBias = new float[HID];
		
		oBias  = new float[OUT];
		osBias = new float[OUT];
		
		eOutput = new float[OUT];
		eHidden = new float[HID];
		
		listeners = new ArrayList<NetworkProgressListener>();
		
		rnd = new Random();
		xavierInitializer();
	}
	
    private float xavierWeight(float standardDeviation) {
        return (float) (rnd.nextGaussian() * standardDeviation);
    }
    
    private float randomBias() {
        return (float) (rnd.nextBoolean() ? -1f : 1f);
    }
    
    protected void xavierInitializer() {
		rnd = new Random();
		
		final float nAvg = (IN + OUT) / 2f;
	    final float variance = 1f / nAvg;
	    final float dev = (float) Math.sqrt(variance);

	    for (int i = 0; i < W.length; i++)
			for (int j = 0; j < W[i].length; j++)
				W[i][j] = xavierWeight(dev);
	    
	    for (int i = 0; i < V.length; i++)
			for (int j = 0; j < V[i].length; j++)
				V[i][j] = xavierWeight(dev);
	    
	    for (int i = 0; i < hBias.length; i++) {        
			hBias[i] = xavierWeight(dev);
			hsBias[i] = randomBias();
	    }
	    
	    for (int i = 0; i < oBias.length; i++) {
			oBias[i] = xavierWeight(dev);
			osBias[i] = randomBias();
	    }
	}

	protected float activation(final float x) {
		return (float) (1 / (1 + Math.exp(-0.2f * x)));
	}

	protected float derivative(final float x) {
		return x * (1 - x);
	}
	
	public void forward(final float[] data) {
		for (int i = 0; i < IN; i++)			
			I[i] = data[i];

		for (int j = 0; j < HID; j++) {
			float sum = 0f;
			final float w[] = W[j];
			
			for (int i = 0; i < IN; i++)
				sum += w[i] * I[i];
			
			H[j] = activation(sum + hBias[j]);
		}

		for (int k = 0; k < OUT; k++) {
			float sum = 0f;
			final float v[] = V[k];
			
			for (int j = 0; j < HID; j++)
				sum += v[j] * H[j];
			
			O[k] = activation(sum + oBias[k]);
		}
	}

	public float back(final Dataset data) {
		float error = 0f;
		
		for (int i = 0; i < OUT; i++)
			Arrays.fill(dV[i], 0f);
		
		for (int i = 0; i < HID; i++)
			Arrays.fill(dW[i], 0f);

		// error in output layer
		for (int k = 0; k < OUT; k++) {
			final float o = O[k];
			final float d = data.output[k] - o;

			error += d * d;
			eOutput[k] = d * derivative(o);
		}

		// error in hidden layer
		for (int j = 0; j < HID; j++) {
			float sum = 0f;
			
			for (int k = 0; k < OUT; k++)
				sum += eOutput[k] * V[k][j];

			eHidden[j] = sum * derivative(H[j]);
		}

		// adjust weights
		for (int k = 0; k < OUT; k++) {
			final float v[] = V[k];
			final float oe = eOutput[k];
			
			final float d[] = dV[k];
			
			for (int j = 0; j < HID; j++) {
				final float dw = oe * H[j];
				final float dm = MOMENTUM * d[j];
				
				v[j] += LEARN_RATE * (dw + dm);
				d[j] = dw;
			}

			oBias[k] += LEARN_RATE * oe * osBias[k];
		}

		for (int j = 0; j < HID; j++) {
			final float w[] = W[j];
			final float he = eHidden[j];
			
			final float d[] = dW[j];

			for (int i = 0; i < IN; i++) {
				final float dw = he * I[i];
				final float dm = MOMENTUM * d[i];
				
				w[i] += LEARN_RATE * (dw + dm);
				d[i] = dw;
			}

			hBias[j] += LEARN_RATE * he * hsBias[j];
		}

		return error;
	}
	
	protected void sendNotification(final String msg) {
		for (final NetworkProgressListener listener: listeners)
			listener.notifyProgress(msg);		
	}
	
	public void batchLearn(final Vector<Vector<Dataset>> batches) {
		log.info("Learing...");
		
		for (int loop = 0; loop < EPOCHS; loop++) {
			float error = 0f;
			int count = 0;
			
			for (final Vector<Dataset> batch: batches) {
				Collections.shuffle(batch);
				final float output[] = new float[OUT];
				
				for (final Enumeration<Dataset> e = batch.elements(); e.hasMoreElements();) {
					final Dataset dataset = e.nextElement();
	
					for (int i = 0; i < IN; i++)
						output[i] += dataset.output[i];
						
					forward(dataset.input);					
				}

				error += back(new Dataset(null, output));
				count++;
			}
			
			error /= count;
			
			if (loop % 100 == 0)				
				sendNotification(loop + ": " + error);

			if (error < ERR_LIMIT)
				break;
		}

		sendNotification("done.");
	}
	
	public void train(final Vector<Dataset> samples) {
		log.info("Learning...");
		
		for (int loop = 0; loop < EPOCHS; loop++) {
			float error = 0f;		
			int count = 0;
			
			Collections.shuffle(samples);
			for (final Enumeration<Dataset> e = samples.elements(); e.hasMoreElements();) {
				final Dataset dataset = e.nextElement();

				forward(dataset.input);
				error += back(dataset);
				count++;
			}
			
			error /= count;
			
			if (loop % 100 == 0)				
				sendNotification(loop + ": " + error);

			if (error < ERR_LIMIT)
				break;
		}

		sendNotification("done.");
	}
	
	public float[] getResult() {
		return this.O;
	}
	
	protected void saveNetwork(final DataOutputStream dos) throws IOException {
	    for (int i = 0; i < W.length; i++)
	    	for (int j = 0; j < I.length; j++)
	    		dos.writeFloat(W[i][j]);

	    for (int i = 0; i < V.length; i++)
	    	for (int j = 0; j < W.length; j++)
	    		dos.writeFloat(V[i][j]);
	    
	    for (int i = 0; i < hBias.length; i++)
	    	dos.writeFloat(hBias[i]);

	    for (int i = 0; i < oBias.length; i++)
	    	dos.writeFloat(oBias[i]);    
	}

	public void save(final OutputStream outputStream) throws IOException {
	    final DataOutputStream dos = new DataOutputStream(outputStream);
	    saveNetwork(dos);
	    
	    dos.close();		
	}
	
	protected void loadNetwork(final DataInputStream dos) throws IOException {
	    for (int i = 0; i < W.length; i++)
	    	for (int j = 0; j < I.length; j++)
	    		W[i][j] = dos.readFloat();

	    for (int i = 0; i < V.length; i++)
	    	for (int j = 0; j < W.length; j++)
	    		V[i][j] = dos.readFloat();
	    
	    for (int i = 0; i < hBias.length; i++)
	    	hBias[i] = dos.readFloat();

	    for (int i = 0; i < oBias.length; i++)
	    	oBias[i] = dos.readFloat();
	}
	
	public void load(final InputStream inputStream) throws IOException {
	    final DataInputStream dos = new DataInputStream(inputStream);
	    loadNetwork(dos);
	    
	    dos.close();		
	}

	@Override
	public void addProgressListener(final NetworkProgressListener listener) {
		listeners.add(listener);
	}
}