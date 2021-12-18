package com.github.mtps.ktor

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

val helloModule = module {
    single<HelloRepository> {
        object : HelloRepository {
            private val map = mutableMapOf<String, String>()
            override fun get(key: String) = map[key]
            override fun set(key: String, value: String) {
                map[key] = value
            }
        }
    }

    single { HelloService(get()) }
}

fun main(args: Array<String>) {
    startKoin {
        modules(helloModule)

        GRPC.embeddedServer(koin.get<HelloService>()).start(wait = true)
    }
}