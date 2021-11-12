package com.github.mtps.ktor

import example.Example.HelloReply
import example.Example.HelloRequest
import example.HelloServiceGrpcKt.HelloServiceCoroutineImplBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HelloService : HelloServiceCoroutineImplBase() {
    private val reply = { request: HelloRequest ->
        HelloReply.newBuilder()
            .setResponse("hello ${request.name}!!")
            .build()
    }

    override suspend fun hello(request: HelloRequest): HelloReply {
        return reply(request)
    }

    override fun hellos(requests: Flow<HelloRequest>): Flow<HelloReply> {
        return requests.map { reply(it) }
    }
}
