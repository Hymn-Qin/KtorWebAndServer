package com.geely.gic.hmi.data.dao

import com.geely.gic.hmi.data.model.Video
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import org.ehcache.impl.config.persistence.CacheManagerPersistenceConfiguration
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * Class that represents [VideoDatabase] of the application.
 * It uses a folder instead of a real database to store videos an indexes,
 */
class VideoDatabase(val uploadDir: File) {
    /**
     * A [GsonBuilder] used for storing the video information in a `.idx` file.
     */
    val gson = GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)
        .create()

    private val resourcePools = ResourcePoolsBuilder.newResourcePoolsBuilder()
        .heap(1000, EntryUnit.ENTRIES)
        .offheap(10, MemoryUnit.MB)
        .disk(100, MemoryUnit.MB, true)
    private val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerPersistenceConfiguration(uploadDir))
        .withCache(
            "videos",
            CacheConfigurationBuilder
                .newCacheConfigurationBuilder(Long::class.javaObjectType, Video::class.java, resourcePools)
        )
        .build(true)

    private val videosCache = cacheManager.getCache("videos", Long::class.javaObjectType, Video::class.java)


    private val digitsOnlyRegex = "\\d+".toRegex()
    private val allIds by lazy {
        uploadDir.listFiles { f ->
            f.extension == "idx" && f.nameWithoutExtension.matches(digitsOnlyRegex)
        }
            .mapTo(ArrayList()) { it.nameWithoutExtension.toLong() }
    }

    /**
     * Stores the last id of this database to provide incremental unique ids.
     */
    val biggestId by lazy { AtomicLong(allIds.max() ?: 0) }

    /**
     * Returns a [Sequence] with all te [Video]s.
     */
    fun listAll(): Sequence<Video> = allIds.asSequence().mapNotNull { videoById(it) }

    /**
     * Returns the first 10 uploaded videos.
     */
    fun top() = listAll().take(10).toList()

    /**
     * Tries to obtain a [Video] from its numeric [id].
     *
     * First tries to search in the cache, and if not available,
     * tries to read it from a file inside the [uploadDir] holding the video metadata.
     */
    fun videoById(id: Long): Video? {
        val video = videosCache.get(id)
        if (video != null) {
            return video
        }

        try {
            val json = gson.fromJson(File(uploadDir, "$id.idx").readText(), Video::class.java)
            videosCache.put(id, json)

            return json
        } catch (e: Throwable) {
            return null
        }
    }

    /**
     * Computes a unique incremental numeric ID for representing a new video.
     */
    fun nextId() = biggestId.incrementAndGet()

    /**
     * Creates a [Video] metadata information with a new unique id, and stores it in disk and the cache.
     */
    fun addVideo(title: String, userId: String, file: File): Long {
        val id = nextId()
        val video = Video(id, title, userId, file.path)

        File(uploadDir, "$id.idx").writeText(gson.toJson(video))
        allIds.add(id)

        videosCache.put(id, video)

        return id
    }
}