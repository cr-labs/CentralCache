package com.challengeandresponse.centralcache;

import net.sf.ehcache.*;
import net.sf.ehcache.config.*;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;


/**
 * An implementation of EHCACHE to simplify configuration
 * of the CacheManager and then the Caches that it controls.
 * 
 * <p>A CentralCache configures a single CacheManager, and any number of caches inside it.</p>
 * 
 * <p>There's also a simplified constructor for new caches with some settings
 * hard-coded.</p>
 * 
 * <p>Sample code:<br />
 * <code>
 * 		CentralCache cc = new CentralCache("/tmp/centralcachetest");<br />
 *		try {<br />
 *			cc.addCache("jimcache",10,false,120,60,120,100);<br />
 *			Ehcache ch = cc.getEhcache("jimcache");<br />
 * 			ch.put(new Element("Oranges","12"));<br />
 *			Element el = ch.get("Oranges");<br />
 *			System.out.println("Found object: "+el);<br />
 *			cc.shutdown();<br />
 *		}<br />
 *		catch (CentralCacheException cce) {<br />
 *			System.out.println("CentralCacheException:"+cce.getMessage());<br />
 *		}<br />
 * </code>
 * 
 * @author jim
 *
 */
public class CentralCache {
	public static final String	PRODUCT_SHORT = "CentralCache";
	public static final String	PRODUCT_LONG = "Challenge/Response CentralCache";
	public static final String	VERSION_SHORT = "0.14 beta";
	public static final String	VERSION_LONG = PRODUCT_LONG + " " + VERSION_SHORT;
	public static final String	COPYRIGHT = "Copyright (c) 2007 Challenge/Response LLC, Cambridge, MA";

	public static final int MINUTES = 60;
	public static final int HOURS = MINUTES * 60;
	public static final int DAYS = HOURS * 24;
	
	private CacheManager cManager;	// the single CacheManager created by and used with all of the caches herein
	private boolean diskPersistent; // true if the cManager was created with a diskStorePath

	/**
	 * Configure an EH CacheManager, with a DiskStore at diskStorePath.
	 * If diskStorePath is null, then the CacheManger is configured with no DiskStore
	 * 
	 * @param diskStorePath the path to the DiskStore, or null if there is to be no DiskStore
	 */
	public CentralCache(String diskStorePath) {
		Configuration conf = new Configuration();
		conf.addDefaultCache(new CacheConfiguration());
		if (diskStorePath != null) {
			DiskStoreConfiguration dsc = new DiskStoreConfiguration();
			dsc.setPath(diskStorePath);
			conf.addDiskStore(dsc);
			diskPersistent = true;
		}
		else
			diskPersistent = false;
		cManager = new CacheManager(conf);
	}

	/**
	 * Get the CacheManager that's running the caches managed here
	 * @return the CacheManager that's running the caches managed here
	 */
	public CacheManager getCacheManager() {
		return cManager;
	}


	/**
	 * Shut down this CentralCache's CacheManager in an orderly way, persisting the cache to disk and so on
	 */
	public void shutdown() {
		cManager.shutdown();
	}


	/**
	 * Add a new Cache to the CacheManager with some obvious defaults.
	 * MemoryStoreEvictionPolicy is LRU; OverflowToDisk is and DiskPersistent are true if the CentralCache was created with a diskStorePath
	 * 
	 * @param name the name of the cache
	 * @param maxElementsInMemory The maximum number of objects that will be maintained in memory for this cache
	 * @param eternal If true, TTL is ignored and the elements never expire
	 * @param timeToLiveSeconds If 0, an object can live forever. This is the max time between creation time and the time an object expires
	 * @param timeToIdleSeconds If 0, an object can idle forever. This is the max time between accesses before an object expires
	 * @param diskExpiryThreadIntervalSeconds Number of seconds between runs of the disk expiry thread. Default is 120 seconds.
	 * @param maxElementsOnDisk If 0, unlimited, otherwise the max number of objects that will be maintained in the DiskStore
	 * @throws CentralCacheException if a cache with the name 'name' is already managed by this CentralCache's CacheManager
	 */
	public void addCache(String name, int maxElementsInMemory,
			boolean eternal, long timeToLiveSeconds, long timeToIdleSeconds, 
			long diskExpiryThreadIntervalSeconds, int maxElementsOnDisk)
	throws CentralCacheException {
		if (cManager.cacheExists(name)) {
			throw new CentralCacheException("Cache already exists: "+name);
		}
		Cache c = new Cache(
				name, maxElementsInMemory, MemoryStoreEvictionPolicy.LRU,
				diskPersistent, null, eternal, timeToLiveSeconds, timeToIdleSeconds,
				diskPersistent, diskExpiryThreadIntervalSeconds,
				null, null, maxElementsOnDisk);
		cManager.addCache(c);
	}

	/**
	 * Add a new SelfPopulatingCache to the CacheManager with obvious defaults.
	 * MemoryStoreEvictionPolicy is LRU; OverflowToDisk is and DiskPersistent are true if the CentralCache was created with a diskStorePath
	 * 
	 * @param name the name of the cache
	 * @param maxElementsInMemory The maximum number of objects that will be maintained in memory for this cache
	 * @param eternal If true, TTL is ignored and the elements never expire
	 * @param timeToLiveSeconds If 0, an object can live forever. This is the max time between creation time and the time an object expires
	 * @param timeToIdleSeconds If 0, an object can idle forever. This is the max time between accesses before an object expires
	 * @param diskExpiryThreadIntervalSeconds Number of seconds between runs of the disk expiry thread. Default is 120 seconds.
	 * @param maxElementsOnDisk If 0, unlimited, otherwise the max number of objects that will be maintained in the DiskStore
	 * @param factory the CacheEntryFactory implementing class whose createEntry() method will block while fetching an item when there is a cache miss
	 * @throws CentralCacheException if a cache with the name 'name' is already managed by this CentralCache's CacheManager
	 */
	public void addSelfPopulatingCache(String name, int maxElementsInMemory,
			boolean eternal, long timeToLiveSeconds, long timeToIdleSeconds, 
			long diskExpiryThreadIntervalSeconds, int maxElementsOnDisk, CacheEntryFactory factory)
	throws CentralCacheException {
		if (cManager.cacheExists(name)) {
			throw new CentralCacheException("Cache already exists: "+name);
		}
		Cache c = new Cache(
				name, maxElementsInMemory, MemoryStoreEvictionPolicy.LRU,
				diskPersistent, null, eternal, timeToLiveSeconds, timeToIdleSeconds,
				diskPersistent, diskExpiryThreadIntervalSeconds,
				null, null, maxElementsOnDisk);
		SelfPopulatingCache spc = new SelfPopulatingCache(c,factory);
		cManager.addCache(spc);
	}


	/**
	 * @param name the Ehcache to retrieve
	 * @return the Ehcache with the given name, managed by this CentralCache's CacheManager, or null if there is no cache with that name
	 * @throws CentralCacheException if something went wrong when getting the Ehcache
	 */
	public Ehcache getEhcache(String name)
	throws CentralCacheException {
		try {
			return cManager.getEhcache(name);
		}
		catch (Exception e) {
			throw new CentralCacheException(e.getMessage());
		}
	}

	/**
	 * Get the RegisteredListeners object for the named cache
	 */
	public RegisteredEventListeners getRegisteredListeners(String name) {
		return cManager.getEhcache(name).getCacheEventNotificationService();
	}

	
	/**
	 * Get Statistics object for a named cache managed by this CentralCache
	 * @param name the name of the cache to get stats for
	 * @return the Statistics object for that cache
	 */
	public Statistics getStatistics(String name) {
		return cManager.getEhcache(name).getStatistics();
	}



	
	// FOR TESTING
	public static void main(String[] args) {
		CentralCache cc = new CentralCache("/tmp/centralcachetest");
		try {
			cc.addCache("jimcache",10,false,120,60,120,100);
			Ehcache ch = cc.getEhcache("jimcache");
			ch.put(new Element("Oranges","12"));
			Element el = ch.get("Oranges");
			System.out.println("Found object: "+el);
			cc.shutdown();
		}
		catch (CentralCacheException cce) {
			System.out.println("CentralCacheException:"+cce.getMessage());
		}
	}
	 



}