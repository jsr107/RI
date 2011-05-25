package javax.cache.implementation;

import javax.cache.Cache;
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheStatisticsMBean;
import javax.cache.listeners.CacheEntryListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * RI
 */
public class RICache<K,V> implements Cache<K,V> {
    private final HashMap<K,V> store = new HashMap<K,V>();

    /**
     * Gets an entry from the cache.
     * <p/>
     * A return value of
     * {@code null} does not <i>necessarily</i> indicate that the map
     * contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to {@code null}.  The {@link #containsKey(Object)}
     * operation may be used to distinguish these two cases.
     *
     * @param key the key whose associated value is to be returned
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException      if the cache is not {@link javax.cache.Status#STARTED}
     * @throws IllegalArgumentException if the key is null
     * @throws javax.cache.CacheException
     */
    public V get(Object key) throws CacheException {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }
        return store.get(key);
    }

    /**
     * The getAll method will return, from the cache, a Map of the objects
     * associated with the Collection of keys in argument "keys". If the objects
     * are not in the cache, the associated cache loader will be called. If no
     * loader is associated with an object, a null is returned.  If a problem
     * is encountered during the retrieving or loading of the objects, an
     * exception will be thrown.
     * <p/>
     * If the "arg" argument is set, the arg object will be passed to the
     * CacheLoader.loadAll method.  The cache will not dereference the object.
     * If no "arg" value is provided a null will be passed to the loadAll
     * method.
     * <p/>
     * The storing of null values in the cache is permitted, however, the get
     * method will not distinguish returning a null stored in the cache and
     * not finding the object in the cache. In both cases a null is returned.
     *
     * @param keys The keys whose associated values are to be returned.
     * @return The entries for the specified keys.
     */
    public Map<K, V> getAll(Collection<? extends K> keys) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns <tt>true</tt> if this cache contains a mapping for the specified
     * key.  More formally, returns <tt>true</tt> if and only if
     * this cache contains a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
     * at most one such mapping.)
     * <p/>
     *
     * @param key key whose presence in this cache is to be tested.
     *            null is permitted but the cache will always return null
     * @return <tt>true</tt> if this map contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * The load method provides a means to "pre load" the cache. This method
     * will, asynchronously, load the specified object into the cache using
     * the associated cacheloader. If the object already exists in the cache,
     * no action is taken. If no loader is associated with the object, no object
     * will be loaded into the cache.  If a problem is encountered during the
     * retrieving or loading of the object, an exception should
     * be logged.
     * <p/>
     * If the "arg" argument is set, the arg object will be passed to the
     * CacheLoader.load method.  The cache will not dereference the object. If
     * no "arg" value is provided a null will be passed to the load method.
     * The storing of null values in the cache is permitted, however, the get
     * method will not distinguish returning a null stored in the cache and not
     * finding the object in the cache. In both cases a null is returned.
     *
     * @param key the key
     * @param specificLoader a specific loader to use. If null the default loader is used.
     * @param loaderArgument provision for additional parameters to be passed to the loader
     * @return a Future which can be used to monitor execution
     */
    public Future load(K key, CacheLoader specificLoader, Object loaderArgument) {
        throw new UnsupportedOperationException();
    }

    /**
     * The loadAll method provides a means to "pre load" objects into the cache.
     * This method will, asynchronously, load the specified objects into the
     * cache using the associated cache loader. If the an object already exists
     * in the cache, no action is taken. If no loader is associated with the
     * object, no object will be loaded into the cache.  If a problem is
     * encountered during the retrieving or loading of the objects, an
     * exception (to be defined) should be logged.
     * <p/>
     * The getAll method will return, from the cache, a Map of the objects
     * associated with the Collection of keys in argument "keys". If the objects
     * are not in the cache, the associated cache loader will be called. If no
     * loader is associated with an object, a null is returned.  If a problem
     * is encountered during the retrieving or loading of the objects, an
     * exception (to be defined) will be thrown.
     * <p/>
     * If the "arg" argument is set, the arg object will be passed to the
     * CacheLoader.loadAll method.  The cache will not dereference the object.
     * If no "arg" value is provided a null will be passed to the loadAll
     * method.
     *
     * @param keys the keys
     * @param specificLoader a specific loader to use. If null the default loader is used.
     * @param loaderArgument provision for additional parameters to be passed to the loader
     * @return a Future which can be used to monitor execution
     */
    public Future loadAll(Collection<? extends K> keys, CacheLoader specificLoader, Object loaderArgument) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the CacheEntry object associated with the object identified by
     * "key". If the object is not in the cache a null is returned.
     */
    public Entry<K,V> getCacheEntry(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the CacheStatistics object associated with the cache.
     * May return null if the cache does not support statistics gathering.
     */
    public CacheStatisticsMBean getCacheStatistics() {
        throw new UnsupportedOperationException();
    }

    /**
     * Add a listener to the list of cache listeners
     */
    public void addListener(CacheEntryListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove a listener from the list of cache listeners
     */
    public void removeListener(CacheEntryListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * NOTE: different return value
     *
     * @see java.util.Map#put(Object, Object)
     */
    public void put(K key, V value) {
        store.put(key, value);
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    /**
     * NOTE: different return value
     *
     * @see java.util.concurrent.ConcurrentMap#putIfAbsent(Object, Object)
     */
    public boolean putIfAbsent(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * NOTE: different return value
     *
     * @return returns false if there was no matching key
     * @see java.util.Map#remove(Object)
     */
    public boolean remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the entry for a key only if currently mapped to a given value.
     * <p/>
     * This is equivalent to
     * <pre>
     *   if (cache.containsKey(key) &amp;&amp; cache.get(key).equals(value)) {
     *       cache.remove(key);
     *       return true;
     *   } else return false;</pre>
     * except that the action is performed atomically.
     *
     * @param key key with which the specified value is associated
     * @return <tt>true</tt> if the value was removed
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation
     *                                       is not supported by this cache
     * @throws ClassCastException            if the key or value is of an inappropriate
     *                                       type for this cache (optional)
     * @throws NullPointerException          if the specified key or value is null,
     *                                       and this cache does not permit null keys or values (optional)
     */
    public V getAndRemove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.ConcurrentMap#replace(Object, Object, Object)
     */
    public boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.ConcurrentMap#replace(Object, Object)
     */
    public boolean replace(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.ConcurrentMap#replace(Object, Object)
     */
    public V getAndReplace(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes all of the mappings from this cache.
     * The cache will be empty after this call returns.
     * <p/>
     * This is potentially an expensive operation.
     * <p/>
     */
    public void removeAll() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the CacheConfiguration, which is immutable
     */
    public CacheConfiguration getConfiguration() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an iterator over a set of elements of type T.
     *
     * @return an Iterator.
     */
    public Iterator<Entry<K, V>> iterator() {
        throw new UnsupportedOperationException();
    }
}
