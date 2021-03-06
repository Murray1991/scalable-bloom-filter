package com.elaunira.sbf;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.zip.GZIPOutputStream;

/**
 * This bloom filter is a variant of a classical bloom filter as explained in
 * the <a href=
 * "http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.153.6902&rep=rep1&type=pdf"
 * >Approximate caches for packet classification</a>. It consists of
 * partitioning the {@code M} bits among the {@code k} hash functions, thus
 * creating {@code k} slices of {@code m = M / k} bits.
 * <p>
 * Using slices result in a more robust filter, with no element specially
 * sensitive to false positives.
 * <p>
 * This class is <strong>not thread-safe</strong>. Moreover, when an element is
 * added into the Bloom filter, it is based on the uniqueness of this object
 * which is defined by the {@link #hashCode()} method. Therefore it is really
 * important to provide a correct {@link #hashCode()} method for elements which
 * have to be passed to the {@link #add} method.
 * 
 * @author Laurent Pellegrino
 * 
 * @version $Id$
 */
public class SlicedBloomFilter<E> extends BloomFilter<E> {

	private static final long serialVersionUID = 1L;
	
	// the number of slices to use (equals to the number 
	// of hash function to use)
	private final int slicesCount;
	
	// the number of bits per slice
	private final int bitsPerSlice;
	
	// the set containing the values for each slice
	private final BitSet filter;

	// the number of elements added in the Bloom filter
	private int count;

	/**
	 * This BloomFilter must be able to store at least {@code capacity} elements
	 * while maintaining no more than {@code falsePositiveProbability} chance of
	 * false positives.
	 * 
	 * @param capacity
	 *            the maximum number of elements the Bloom filter can contain
	 *            without to transcend the {@code falsePositiveProbability}.
	 * 
	 * @param falsePositiveProbability
	 *            the maximum false positives rate allowed by this filter.
	 */
	public SlicedBloomFilter(int capacity, double falsePositiveProbability) {
		super(capacity, falsePositiveProbability);
		
		this.slicesCount = 
			BloomFilterUtil.computeSlicesCount(
					capacity, falsePositiveProbability);
		
		this.bitsPerSlice = 
			BloomFilterUtil.computeBitsPerSlice(
					capacity, falsePositiveProbability, this.slicesCount);

		this.filter = new BitSet(this.slicesCount * this.bitsPerSlice);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean add(E elt) {
		if (this.contains(elt)) {
			return true;
		}
		
		this.addWithoutCheck(elt);
		
		return false;
	}

	/**
	 * Adds the specified element without verifying that the element is
	 * contained by the Bloom filter. The size of the Bloom filter is
	 * incremented by one even if the element is already contained by the
	 * filter. Therefore, this method should only be used if you know what you
	 * do.
	 * 
	 * @param elt
	 *            the element to add to the Bloom filter.
	 */
	public void addWithoutCheck(E elt) {
		if (this.isFull()) {
			throw new IllegalStateException("bloom filter is at capacity");
		}
		
		int[] hashes = 
			BloomFilterUtil.getHashBuckets(
					Integer.toString(elt.hashCode()), 
					this.slicesCount, this.bitsPerSlice);
		
		int offset = 0;
		for (int k : hashes) {
			this.filter.set(offset + k);
			offset += this.bitsPerSlice;
		}
		
		this.count++;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean contains(E elt) {
		int[] hashes = 
			BloomFilterUtil.getHashBuckets(
					Integer.toString(elt.hashCode()),
					this.slicesCount, this.bitsPerSlice);
		
		int offset = 0;
		for (int k : hashes) {
			if (!this.filter.get(offset + k)) {
				return false;
			}
			offset += this.bitsPerSlice;
		}
		
		return true;
	}
	
	/**
	 * Returns a boolean indicating if the Bloom filter has reached its maximal
	 * capacity.
	 * 
	 * @return {@code true} whether the Bloom filter has reached its maximal
	 *         capacity, {@code false} otherwise.
	 */
	public boolean isFull() {
		return this.count > this.capacity;
	}
	
	/**
	 * Returns the number of elements added in this Bloom filter.
	 * 
	 * @return the number of elements added in this Bloom filter.
	 */
	public int size() {
		return this.count;
	}
	
	/**
	 * Returns the number of bits per slice.
	 * 
	 * @return the number of bits per slice.
	 */
	public int getBitsPerSlice() {
		return bitsPerSlice;
	}
	
	/**
	 * Returns the number of slices associated to this filter.
	 * 
	 * @return the number of slices associated to this filter.
	 */
	public int getSlicesCount() {
		return slicesCount;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return super.toString() + 
			"[slicesCount=" + this.slicesCount + ", bitsPerSlice=" + this.bitsPerSlice + "]";
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		GZIPOutputStream gzipos = new GZIPOutputStream(out);
		ObjectOutputStream oos = new ObjectOutputStream(gzipos);
		
		oos.writeInt(super.capacity);
		oos.writeDouble(super.falsePositiveProbability);
		oos.writeObject(this.filter);
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {

	}

}
