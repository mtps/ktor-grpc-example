package com.github.mtps.ktor

import ch.qos.logback.classic.Level.ERROR
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.asSuspending
import io.grpc.BindableService
import io.grpc.protobuf.services.ProtoReflectionService
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import java.util.concurrent.TimeUnit
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import org.slf4j.LoggerFactory

val helloModule = module {
	single { dbConnection.connect().get(10, TimeUnit.SECONDS) }
	factory { get<Connection>().asSuspending }
	single<HelloRepository> { HelloMapRepository() }
}

var org.slf4j.Logger.level: ch.qos.logback.classic.Level
	get() = (this as ch.qos.logback.classic.Logger).level
	set(value) { (this as ch.qos.logback.classic.Logger).level = value }

typealias KtorModule = Application.() -> Unit

fun main(args: Array<String>) {
	startKoin() {

		LoggerFactory.getLogger("io.grpc.netty").level = ERROR
		LoggerFactory.getLogger("io.netty").level = ERROR

		logger(SLF4JLogger(Level.ERROR))
		//printLogger(Level.ERROR)

		modules(helloModule)

		val grpcServices = listOf(
			HelloService.newInstance(koin.get()),
			ProtoReflectionService.newInstance(),
		)

		koin.getAll<BindableService>()

		val module: KtorModule = {
			install(ContentNegotiation) { gson {} }
		}

		val server = GRPC.embeddedServer(
			services = grpcServices,
			module = module,
		)

		server.start(wait = true)
	}
}