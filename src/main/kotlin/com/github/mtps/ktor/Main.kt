package com.github.mtps.ktor

fun main(args: Array<String>) {
    GRPC.embeddedServer(HelloService()).start(wait = true)
}

