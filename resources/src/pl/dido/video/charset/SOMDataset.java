package pl.dido.video.charset;

import java.util.ArrayList;

import cern.colt.bitvector.BitVector;

public class SOMDataset {
	protected final ArrayList<BitVector> set = new ArrayList<BitVector>(10);
	protected int index = 0;
	
	public void add(final BitVector vec) {
		set.add(vec);
	}
	
	public BitVector getNext() {
		if (index < set.size())
			return set.get(index++);
		
		return null;
	}
	
	public void reset() {
		index = 0;
	}
	
	public int size() {
		return set.size();
	}

	public void addAll(final SOMDataset dataset) {
		set.addAll(dataset.set);
	}
}
