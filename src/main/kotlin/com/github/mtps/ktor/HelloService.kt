package com.github.mtps.ktor

import com.google.protobuf.Empty
import example.Example
import example.Example.HelloReply
import example.Example.HelloRequest
import example.HelloServiceGrpcKt.HelloServiceCoroutineImplBase
import io.ktor.features.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface HelloRepository {
    operator fun get(key: String): String?
    operator fun set(key: String, value: String)
}

class HelloService(private val helloRepository: HelloRepository) : HelloServiceCoroutineImplBase() {
    private val reply = { request: HelloRequest ->
        val item = helloRepository[request.name]
            ?: throw NotFoundException()

        HelloReply.newBuilder()
            .setResponse("hello ${request.name}!! -> $item")
            .build()
    }

    override suspend fun hello(request: HelloRequest): HelloReply {
        return reply(request)
    }

    override fun hellos(requests: Flow<HelloRequest>): Flow<HelloReply> {
        return requests.map { reply(it) }
    }

    override suspend fun saveHello(request: Example.HelloSaveRequest): Empty {
        helloRepository[request.name] = request.value
        return Empty.getDefaultInstance()
    }
}