package com.github.mtps.ktor

import com.github.jasync.sql.db.SuspendingConnection

interface HelloRepository {
    suspend fun get(key: String): String?
    suspend fun set(key: String, value: String)
}

class HelloMapRepository(private val map: MutableMap<String, String> = mutableMapOf()) : HelloRepository {
    override suspend fun get(key: String) = map[key]
    override suspend fun set(key: String, value: String) { map[key] = value }
}

class HelloPGRepository(private val connection: SuspendingConnection) : HelloRepository {
    override suspend fun get(key: String): String? {
        val queryResult = connection.sendPreparedStatement("select * from user")
        return ""
    }

    override suspend fun set(key: String, value: String) {
        TODO("Not yet implemented")
    }
}
