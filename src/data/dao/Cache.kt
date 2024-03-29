package com.geely.gic.hmi.data.dao

import com.geely.gic.hmi.data.model.*
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.*
import org.ehcache.impl.config.persistence.CacheManagerPersistenceConfiguration
import org.joda.time.*
import java.io.*

/**
 * An Ehcache based implementation for the [DAOFacade] that uses a [delegate] facade and a [storagePath]
 * and perform several caching strategies for each domain operation.
 */
class DAOFacadeCache(private val delegate: DAOFacade, private val storagePath: File) : DAOFacade {

    private val resourcePools = ResourcePoolsBuilder.newResourcePoolsBuilder()
        .heap(1000, EntryUnit.ENTRIES)
        .offheap(10, MemoryUnit.MB)
        .disk(100, MemoryUnit.MB, true)
    /**
     * Build a cache manager with a cache for kweets and other for users.
     * It uses the specified [storagePath] for persistence.
     * Limits the cache to 1000 entries, 10MB in memory, and 100MB in disk per both caches.
     */
    private val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerPersistenceConfiguration(storagePath))
        .withCache(
            "kweetsCache",
            CacheConfigurationBuilder
                .newCacheConfigurationBuilder(Int::class.javaObjectType, Kweet::class.java, resourcePools)
        )
        .withCache(
            "usersCache",
            CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String::class.java, User::class.java, resourcePools)
        )
        .build(true)

    /**
     * Gets the cache for kweets represented by an [Int] key and a [Kweet] value.
     */
    private val kweetsCache = cacheManager.getCache("kweetsCache", Int::class.javaObjectType, Kweet::class.java)

    /**
     * Gets the cache for users represented by a [String] key and a [User] value.
     */
    private val usersCache = cacheManager.getCache("usersCache", String::class.java, User::class.java)

    override fun init() {
        delegate.init()
    }

    override fun countReplies(id: Int): Int {
        return delegate.countReplies(id)
    }

    override fun createKweet(user: String, text: String, replyTo: Int?, date: DateTime): Int {
        val id = delegate.createKweet(user, text, replyTo)
        val kweet = Kweet(id, user, text, date, replyTo)
        kweetsCache.put(id, kweet)
        return id
    }

    override fun deleteKweet(id: Int) {
        delegate.deleteKweet(id)
        kweetsCache.remove(id)
    }

    override fun getKweet(id: Int): Kweet {
        // Returns a cached Kweet when available in the cache.
        val cached = kweetsCache.get(id)
        if (cached != null) {
            return cached
        }

        // If not available, we it get from the delegate and store it in the cache so we can access it later.
        val kweet = delegate.getKweet(id)
        kweetsCache.put(id, kweet)

        return kweet
    }

    override fun userKweets(userId: String): List<Int> {
        return delegate.userKweets(userId)
    }

    override fun user(userId: String, hash: String?): User? {
        // Returns a cached User when available in the cache.
        val cached = usersCache.get(userId)
        val user = if (cached == null) {
            val dbUser = delegate.user(userId)
            if (dbUser != null) {
                usersCache.put(userId, dbUser)
            }
            dbUser
        } else {
            cached
        }

        // Verifies that, if specified, the hash matches to return the user.
        return when {
            user == null -> null
            hash == null -> user
            user.passwordHash == hash -> user
            else -> null
        }
    }

    override fun userByEmail(email: String): User? {
        return delegate.userByEmail(email)
    }

    override fun createUser(user: User) {
        if (usersCache.get(user.userId) != null) {
            throw IllegalStateException("User already exist")
        }

        delegate.createUser(user)
        usersCache.put(user.userId, user)
    }

    override fun updateUser(user: User) {
        val cached = usersCache.get(user.userId)

        if (cached != null) {
            delegate.updateUser(user)
            usersCache.put(user.userId, user)
        }
    }

    override fun top(count: Int): List<Int> {
        return delegate.top(count)
    }

    override fun latest(count: Int): List<Int> {
        return delegate.latest(count)
    }

    override fun close() {
        cacheManager.use {
            delegate.close()
        }
    }
}
