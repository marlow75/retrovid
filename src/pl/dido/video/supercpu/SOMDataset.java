package pl.dido.video.supercpu;

import java.util.ArrayList;
import pl.dido.image.utils.BitVector;

public class SOMDataset {
	protected ArrayList<BitVector> set = new ArrayList<BitVector>(100);
	protected int index = 0;
	
	public SOMDataset() {}
	
	public SOMDataset(final ArrayList<BitVector> set) {
		this.set = set;
	}
	
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

	public SOMDataset[] divide() {
		final int size = set.size() / 2;
		
		final ArrayList<BitVector> set1 = new ArrayList<BitVector>(10);
		final ArrayList<BitVector> set2 = new ArrayList<BitVector>(10);
		
		for (int i = 0; i < size; i++)
			set1.add(set.get(i));
		
		for (int i = size; i < 2 * size; i++)
			set2.add(set.get(i));
		
		return new SOMDataset[] { new SOMDataset(set1), new SOMDataset(set2) };
	}
}
