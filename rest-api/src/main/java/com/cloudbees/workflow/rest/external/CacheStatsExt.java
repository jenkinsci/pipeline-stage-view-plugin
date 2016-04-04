package com.cloudbees.workflow.rest.external;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;

/**
 * Represents information about a Guava caceh in use
 * @author <a href="mailto:samvanoort@gmail.com">samvanoort@gmail.com</a>
 */
public class CacheStatsExt {

    // Basics from the cache stats object
    protected long hitCount=0;
    protected long missCount=0;
    protected long loadSuccessCount=0;
    protected long loadExceptionCount=0;
    protected long totalLoadTime=0;
    protected long evictionCount=0;

    protected long cacheEntryCount=0;

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public void setMissCount(long missCount) {
        this.missCount = missCount;
    }

    public long getLoadSuccessCount() {
        return loadSuccessCount;
    }

    public void setLoadSuccessCount(long loadSuccessCount) {
        this.loadSuccessCount = loadSuccessCount;
    }

    public long getLoadExceptionCount() {
        return loadExceptionCount;
    }

    public void setLoadExceptionCount(long loadExceptionCount) {
        this.loadExceptionCount = loadExceptionCount;
    }

    public long getTotalLoadTime() {
        return totalLoadTime;
    }

    public void setTotalLoadTime(long totalLoadTime) {
        this.totalLoadTime = totalLoadTime;
    }

    public long getEvictionCount() {
        return evictionCount;
    }

    public void setEvictionCount(long evictionCount) {
        this.evictionCount = evictionCount;
    }

    public long getCacheEntryCount() {
        return cacheEntryCount;
    }

    public void setCacheEntryCount(long cacheEntryCount) {
        this.cacheEntryCount = cacheEntryCount;
    }

    protected CacheStatsExt() {
    }

    public static CacheStatsExt create(Cache cache) {
        return new CacheStatsExt(cache);
    }

    public CacheStatsExt(Cache cache) {
        CacheStats stats = cache.stats();
        this.setHitCount(stats.hitCount());
        this.setMissCount(stats.missCount());
        this.setLoadSuccessCount(stats.loadSuccessCount());
        this.setTotalLoadTime(stats.loadSuccessCount());
        this.setEvictionCount(stats.evictionCount());
        this.setCacheEntryCount(cache.size());
    }
}
