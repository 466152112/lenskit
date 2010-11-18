/**
 * 
 */
package org.grouplens.reflens.util;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleCollection;
import it.unimi.dsi.fastutil.doubles.AbstractDoubleIterator;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.longs.AbstractLong2DoubleMap;
import it.unimi.dsi.fastutil.longs.AbstractLongCollection;
import it.unimi.dsi.fastutil.longs.AbstractLongIterator;
import it.unimi.dsi.fastutil.longs.AbstractLongSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.AbstractObjectIterator;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class CollectionUtils {
	public static Long2DoubleMap getFastMap(Map<Long,Double> map) {
		if (map instanceof Long2DoubleMap)
			return (Long2DoubleMap) map;
		else
			return new Long2DoubleMapWrapper(map);
	}
	
	public static LongCollection getFastCollection(final Collection<Long> longs) {
		if (longs instanceof LongCollection) return (LongCollection) longs;
		
		return new AbstractLongCollection() {
			private Collection<Long> base = longs;
			@Override
			public int size() {
				return base.size();
			}
			
			@Override
			public boolean contains(long key) {
				return base.contains(key);
			}
			
			@Override
			public LongIterator iterator() {
				return fastIterator(base.iterator());
			}
		};
	}
	
	/**
	 * @return
	 */
	private static LongIterator fastIterator(final Iterator<Long> iter) {
		return new AbstractLongIterator() {
			Iterator<Long> biter = iter;
			@Override
			public boolean hasNext() {
				return biter.hasNext(); 
			}
			@Override
			public Long next() {
				return biter.next();
			}
		};
	}
	
	public static DoubleCollection getFastCollection(final Collection<Double> longs) {
		return new AbstractDoubleCollection() {
			private Collection<Double> base = longs;
			@Override
			public int size() {
				return base.size();
			}
			
			@Override
			public boolean contains(double key) {
				return base.contains(key);
			}
			
			@Override
			public DoubleIterator iterator() {
				return new AbstractDoubleIterator() {
					Iterator<Double> biter = base.iterator();
					@Override
					public boolean hasNext() {
						return biter.hasNext(); 
					}
					@Override
					public Double next() {
						return biter.next();
					}
				};
			}
		};
	}
	
	public static LongSet fastSet(final Set<Long> set) {
		if (set instanceof LongSet) return (LongSet) set;
		
		return new AbstractLongSet() {
			Set<Long> base = set;
			@Override
			public int size() {
				return base.size();
			}
			
			@Override
			public boolean contains(long key) {
				return base.contains(key);
			}
			
			@Override
			public LongIterator iterator() {
				return fastIterator(base.iterator());
			}
		};
	}
	
	private static class Long2DoubleMapWrapper implements Long2DoubleMap {
		private Map<Long, Double> base;
		private double defaultReturn = 0.0;

		public Long2DoubleMapWrapper(Map<Long,Double> base) {
			this.base = base;
		}

		@Override
		public boolean containsValue(double value) {
			return base.containsValue(value);
		}

		@Override
		public ObjectSet<Map.Entry<Long, Double>> entrySet() {
			return new AbstractObjectSet<Map.Entry<Long,Double>>() {
				Set<Map.Entry<Long, Double>> entries = base.entrySet();

				@Override
				public ObjectIterator<Map.Entry<Long, Double>> iterator() {
					return new AbstractObjectIterator<Map.Entry<Long,Double>>() {
						Iterator<Map.Entry<Long, Double>> biter = entries.iterator();
						@Override
						public boolean hasNext() {
							return biter.hasNext();
						}

						@Override
						public Map.Entry<Long, Double> next() {
							return biter.next();
						}
						
					};
				}

				@Override
				public boolean contains(Object o) {
					return entries.contains(o);
				}

				@Override
				public int size() {
					return entries.size();
				}
				
			};
		}

		@Override
		public LongSet keySet() {
			return fastSet(base.keySet());
		}

		@Override
		public ObjectSet<Entry> long2DoubleEntrySet() {
			return new AbstractObjectSet<Entry>() {
				Set<Map.Entry<Long,Double>> entries = base.entrySet();

				@Override
				public ObjectIterator<Entry> iterator() {
					return new AbstractObjectIterator<Entry>() {
						Iterator<Map.Entry<Long,Double>> biter = entries.iterator();

						@Override
						public boolean hasNext() {
							return biter.hasNext();
						}

						@Override
						public Entry next() {
							Map.Entry<Long,Double> entry = biter.next();
							return new AbstractLong2DoubleMap.BasicEntry(
									entry.getKey(), entry.getValue());
						}
						
					};
				}

				@Override
				public boolean contains(Object o) {
					return base.containsKey(o);
				}

				@Override
				public int size() {
					return entries.size();
				}
			};
		}

		@Override
		public DoubleCollection values() {
			return CollectionUtils.getFastCollection(base.values());
		}

		@Override
		public boolean containsKey(long key) {
			return base.containsKey(key);
		}

		@Override
		public double defaultReturnValue() {
			return defaultReturn;
		}

		@Override
		public void defaultReturnValue(double rv) {
			defaultReturn = rv;
		}

		@Override
		public double get(long key) {
			return base.get(key);
		}

		@Override
		public double put(long key, double value) {
			return base.put(key, value);
		}

		@Override
		public double remove(long key) {
			return base.remove(key);
		}

		@Override
		public void clear() {
			base.clear();
		}

		@Override
		public boolean containsKey(Object key) {
			return base.containsKey(key);
		}

		@Override
		public Double get(Object key) {
			return base.get(key);
		}

		@Override
		public Double put(Long key, Double value) {
			return base.put(key, value);
		}

		@Override
		public Double remove(Object key) {
			return base.remove(key);
		}

		@Override
		public int size() {
			return base.size();
		}

		@Override
		public boolean containsValue(Object value) {
			return base.containsValue(value);
		}

		@Override
		public boolean isEmpty() {
			return base.isEmpty();
		}

		@Override
		public void putAll(Map<? extends Long, ? extends Double> m) {
			base.putAll(m);
		}	
	}
}
