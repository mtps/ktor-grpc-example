package com.github.mtps.ktor

import com.github.jasync.sql.db.SuspendingConnection

interface HelloRepository: Iterable<Pair<String, String>> {
    suspend fun get(key: String): String?
    suspend fun set(key: String, value: String)
}

class HelloMapRepository(private val map: MutableMap<String, String> = mutableMapOf()) : HelloRepository {
    override suspend fun get(key: String) = map[key]
    override suspend fun set(key: String, value: String) {
        map[key] = value
    }

    override fun iterator(): Iterator<Pair<String, String>> =
        map.iterator().asSequence().map { it.key to it.value }.iterator()
}

class HelloPGRepository(private val connection: SuspendingConnection) : HelloRepository {
    override suspend fun get(key: String): String? {
        val queryResult = connection.sendPreparedStatement("select * from user")
        return ""
    }

    override suspend fun set(key: String, value: String) {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<Pair<String, String>> {
        TODO("Not yet implemented")
    }
}
