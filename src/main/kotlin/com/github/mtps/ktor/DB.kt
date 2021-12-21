package com.github.mtps.ktor

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.pool.PoolConfiguration
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import kotlinx.coroutines.future.await
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

suspend fun Connection.sendPreparedStatementAwait(query: String, values: List<Any> = emptyList()): QueryResult =
    sendPreparedStatement(query, values).await()

@OptIn(ExperimentalTime::class)
val dbPoolConfig = PoolConfiguration(
    maxObjects = 100,
    maxIdle = 15.minutes.inWholeMilliseconds,
    maxQueueSize = 10_000,
    validationInterval = 30.seconds.inWholeMilliseconds,

    )
val dbConfig = Configuration(username = "postgres", password = "password1", database = "test")

val dbConnection = ConnectionPool(
    PostgreSQLConnectionFactory(dbConfig),
    ConnectionPoolConfiguration(
        maxIdleTime = dbPoolConfig.maxIdle,
        maxPendingQueries = dbPoolConfig.maxQueueSize,
        connectionValidationInterval = dbPoolConfig.validationInterval,
        maxActiveConnections = 10,
    )
)