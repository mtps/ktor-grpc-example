package com.github.mtps.ktor

import com.google.protobuf.Empty
import example.Example.HelloListReply
import example.Example.HelloReply
import example.Example.HelloRequest
import example.Example.HelloSaveRequest
import example.HelloServiceGrpcKt.HelloServiceCoroutineImplBase
import io.ktor.features.NotFoundException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

class HelloService(
    private val helloRepository: HelloRepository,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
) : HelloServiceCoroutineImplBase(coroutineContext) {
    companion object {
        fun newInstance(helloRepository: HelloRepository) = HelloService(helloRepository)
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

    override fun listHello(request: Empty): Flow<HelloListReply> {
        return helloRepository.iterator().asFlow().map { p ->
            HelloListReply.newBuilder().also {
                it.name = p.first
                it.value = p.second
            }.build()
        }
    }
}