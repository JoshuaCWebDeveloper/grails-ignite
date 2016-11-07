package org.grails.ignite

import grails.util.Holders
import groovy.util.logging.Log4j
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheMemoryMode
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.CacheWriteSynchronizationMode
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy
import org.apache.ignite.configuration.CacheConfiguration

/**
 * Created by dstieglitz on 2/21/16.
 */
@Log4j
class IgniteCacheConfigurationFactory {

    static CacheConfiguration getCacheConfiguration(String name) {
        return getCacheConfiguration("ignite.defaultCache", name);
    }

    static CacheConfiguration getCacheConfiguration(String prefix, String name) {
        log.debug "getCacheConfiguration ${prefix}, ${name}"
        def flatConfig = Holders.config.flatten()

        def cacheMode = getConfigValueOrThrowException("${prefix}.cacheMode", flatConfig.get("${prefix}.cacheMode".toString()))
        if (cacheMode == null) {
            cacheMode = getDefaultCacheConfiguration(name).cacheMode
        }
        log.debug "cacheMode=${cacheMode}"

        def memoryMode = getConfigValueOrThrowException("${prefix}.memoryMode", flatConfig.get("${prefix}.memoryMode".toString()))
        if (memoryMode == null) {
            memoryMode = getDefaultCacheConfiguration(name).memoryMode
        }
        log.debug "memoryMode=${memoryMode}"

        def atomicityMode = getConfigValueOrThrowException("${prefix}.atomicityMode", flatConfig.get("${prefix}.atomicityMode".toString()))
        if (atomicityMode == null) {
            atomicityMode = getDefaultCacheConfiguration(name).atomicityMode
        }
        log.debug "atomicityMode=${atomicityMode}"

        def writeSynchronizationMode = getConfigValueOrThrowException("${prefix}.writeSynchronizationMode", flatConfig.get("${prefix}.writeSynchronizationMode".toString()))
        if (writeSynchronizationMode == null) {
            writeSynchronizationMode = getDefaultCacheConfiguration(name).writeSynchronizationMode
        }
        log.debug "writeSynchronizationMode=${writeSynchronizationMode}"

        def offHeapMaxMemory = getConfigValueOrThrowException("${prefix}.offHeapMaxMemory", flatConfig.get("${prefix}.offHeapMaxMemory".toString()))
        if (offHeapMaxMemory == null) {
            offHeapMaxMemory = getDefaultCacheConfiguration(name).offHeapMaxMemory
        }
        log.debug "offHeapMaxMemory=${offHeapMaxMemory}"

        def maxElements = getConfigValueOrThrowException("${prefix}.maxElements", flatConfig.get("${prefix}.maxElements".toString()))
        if (maxElements == null) {
            maxElements = getConfigValueOrZero("ignite.defaultCache.maxElements", flatConfig.get("ignite.defaultCache.maxElements".toString()))
        }
        log.debug "maxElements=${maxElements}"

        def swapEnabled = getConfigValueOrThrowException("${prefix}.swapEnabled", flatConfig.get("${prefix}.swapEnabled".toString()))
        if (swapEnabled == null) {
            swapEnabled = getDefaultCacheConfiguration(name).swapEnabled
        }
        log.debug "swapEnabled=${swapEnabled}"

        def evictionPolicy = getConfigValueOrThrowException("${prefix}.evictionPolicy", flatConfig.get("${prefix}.evictionPolicy".toString()))
        if (evictionPolicy == null) {
            evictionPolicy = 'lru'
        }
        log.debug "evictionPolicy=${evictionPolicy}"

        def statisticsEnabled = getConfigValueOrThrowException("${prefix}.statisticsEnabled", flatConfig.get("${prefix}.statisticsEnabled".toString()))
        if (statisticsEnabled == null) {
            statisticsEnabled = getDefaultCacheConfiguration(name).statisticsEnabled
        }
        log.debug "statisticsEnabled=${statisticsEnabled}"

        def managementEnabled = getConfigValueOrThrowException("${prefix}.managementEnabled", flatConfig.get("${prefix}.managementEnabled".toString()))
        if (managementEnabled == null) {
            managementEnabled = getDefaultCacheConfiguration(name).managementEnabled
        }
        log.debug "managementEnabled=${managementEnabled}"

        if (evictionPolicy != 'lru') {
            throw new IllegalArgumentException("Eviction policy ${evictionPolicy} not supported")
        }

        return getCacheConfigurationLru(name,
                cacheMode,
                memoryMode,
                atomicityMode,
                writeSynchronizationMode,
                (long) offHeapMaxMemory,
                maxElements,
                swapEnabled,
                statisticsEnabled,
                managementEnabled)

        // TODO get this from configuration
        //return getDefaultCacheConfiguration(name)
    }

    /**
     * Sensible defaults
     * @param name
     * @return
     */
    static CacheConfiguration getDefaultCacheConfiguration(String name) {
        def config = new CacheConfiguration(name);
        config.setCacheMode(CacheMode.PARTITIONED)
        config.setMemoryMode(CacheMemoryMode.OFFHEAP_TIERED)
        config.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_ASYNC)
        config.setAtomicityMode(CacheAtomicityMode.ATOMIC)
        config.setOffHeapMaxMemory(1L * 1024L * 1024L * 1024L); // 1 GIG
        config.setEvictionPolicy(new LruEvictionPolicy(10000));
        config.setSwapEnabled(true)
        config.setStatisticsEnabled(false)
        config.setManagementEnabled(false)

        return config
    }

    /**
     * Sensible defaults
     * @param name
     * @return
     */
    static CacheConfiguration getCacheConfigurationLru(String name,
                                                       CacheMode cacheMode,
                                                       CacheMemoryMode cacheMemoryMode,
                                                       CacheAtomicityMode cacheAtomicityMode,
                                                       CacheWriteSynchronizationMode synchronizationMode,
                                                       long offHeapMaxMemory,
                                                       int maxElements,
                                                       boolean swapEnabled,
                                                       boolean statisticsEnabled,
                                                       boolean managementEnabled) {
        def config = new CacheConfiguration(name);
        config.setCacheMode(cacheMode)
        config.setMemoryMode(cacheMemoryMode)
        config.setWriteSynchronizationMode(synchronizationMode)
        config.setAtomicityMode(cacheAtomicityMode)
        config.setOffHeapMaxMemory(offHeapMaxMemory);
        config.setEvictionPolicy(new LruEvictionPolicy(maxElements));
        config.setSwapEnabled(swapEnabled)
        config.setStatisticsEnabled(statisticsEnabled)
        config.setManagementEnabled(managementEnabled)

        return config
    }

    private static def getConfigValueOrThrowException(name, val) {
        log.debug "getConfigValueOrThrowException ${val} for ${name}"

        if (val instanceof ConfigObject) {
            throw new IllegalArgumentException("Config parameter ${name} must be defined")
        } else {
            return val
        }
    }

    private static def getConfigValueOrNull(name, val) {
        log.debug "getConfigValueOrThrowException ${val} for ${name}"

        if (val instanceof ConfigObject) {
            return null
        } else {
            return val
        }
    }

    private static def getConfigValueOrZero(name, val) {
        log.debug "getConfigValueOrThrowException ${val} for ${name}"

        if (val == null || val instanceof ConfigObject) {
            return 0
        } else {
            return val
        }
    }
}