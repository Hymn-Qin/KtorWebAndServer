package com.geely.gic.hmi.data.dao

import com.geely.gic.hmi.data.model.Kweet
import com.geely.gic.hmi.data.model.User
import org.joda.time.DateTime
import java.io.Closeable

/**
 * A DAO Facade interface for the Database. This allows to provide several implementations.
 *
 * In this case this is used to provide a Database-based implementation using Exposed,
 * and a cache implementation composing another another DAOFacade.
 */
interface DAOFacade : Closeable {
    /**
     * Initializes all the required data.
     * In this case this should initialize the Users and Kweets tables.
     */
    fun init()

    /**
     * Counts the number of replies of a kweet identified by its [id].
     */
    fun countReplies(id: Int): Int

    /**
     * Creates a Kweet from a specific [user] name, the kweet [text] content,
     * an optional [replyTo] id of the parent kweet, and a [date] that would default to the current time.
     */
    fun createKweet(user: String, text: String, replyTo: Int? = null, date: DateTime = DateTime.now()): Int

    /**
     * Deletes a kweet from its [id].
     */
    fun deleteKweet(id: Int)

    /**
     * Get the DAO object representation of a kweet based from its [id].
     */
    fun getKweet(id: Int): Kweet

    /**
     * Obtains a list of integral ids of kweets from a specific user identified by its [userId].
     */
    fun userKweets(userId: String): List<Int>

    /**
     * Tries to get an user from its [userId] and optionally its password [hash].
     * If the [hash] is specified, the password [hash] must match, or the function will return null.
     * If no [hash] is specified, it will return the [User] if exists, or null otherwise.
     */
    fun user(userId: String, hash: String? = null): User?

    /**
     * Tries to get an user from its [email].
     *
     * Returns null if no user has this [email] associated.
     */
    fun userByEmail(email: String): User?

    /**
     * Creates a new [user] in the database from its object [User] representation.
     */
    fun createUser(user: User)

    fun updateUser(user: User)
    /**
     * Returns a list of Kweet ids, with the ones with most replies first.
     */
    fun top(count: Int = 10): List<Int>

    /**
     * Returns a list of Keet ids, with the recent ones first.
     */
    fun latest(count: Int = 10): List<Int>
}