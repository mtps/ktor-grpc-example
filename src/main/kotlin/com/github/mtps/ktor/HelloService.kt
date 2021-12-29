package com.github.mtps.ktor

import com.google.protobuf.Empty
import example.Example.HelloReply
import example.Example.HelloRequest
import example.HelloServiceGrpcKt.HelloServiceCoroutineImplBase
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HelloService(private val helloRepository: HelloRepository) : HelloServiceCoroutineImplBase() {
    companion object {
        fun newInstance() = HelloService()
    }

    private suspend fun reply(request: HelloRequest): HelloReply {
        val item = helloRepository.get(request.name)
            ?: throw NotFoundException()

        return HelloReply.newBuilder()
            .setResponse("hello ${request.name}!! -> $item")
            .build()
    }

    override suspend fun hello(request: HelloRequest): HelloReply {
        return reply(request)
    }

    override fun hellos(requests: Flow<HelloRequest>): Flow<HelloReply> {
        return requests.map { reply(it) }
    }

    override suspend fun saveHello(request: HelloSaveRequest): Empty {
        helloRepository.set(request.name, request.value)
        return Empty.getDefaultInstance()
    }
}