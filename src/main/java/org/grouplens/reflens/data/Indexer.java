package org.grouplens.reflens.data;

import org.grouplens.reflens.data.generic.GenericIndexer;

import com.google.inject.ImplementedBy;

@ImplementedBy(GenericIndexer.class)
public interface Indexer<I> {
	public int getIndex(I obj);
	public int getIndex(I obj, boolean insert);
	public I getObject(int idx);
	public int getObjectCount();
}
