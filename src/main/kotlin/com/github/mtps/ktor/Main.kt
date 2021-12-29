package com.github.mtps.ktor

<<<<<<< Updated upstream
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.asSuspending
import io.grpc.protobuf.services.ProtoReflectionService
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val helloModule = module {
    single { dbConnection.connect().get(10, TimeUnit.SECONDS) }
    factory{ get<Connection>().asSuspending }
    single<HelloRepository> { HelloPGRepository(get()) }
    single { HelloService(get()) }
}

fun main(args: Array<String>) {
    startKoin {
        modules(helloModule)

        GRPC.embeddedServer(koin.get<HelloService>(), ProtoReflectionService.newInstance()).start(wait = true)
    }
=======
import io.grpc.protobuf.services.ProtoReflectionService

fun main() {
    val server = GRPC.embeddedServer(
        HelloService.newInstance(),
        ProtoReflectionService.newInstance()
    )

    server.start(wait = true)
>>>>>>> Stashed changes
}