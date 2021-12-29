package com.github.mtps.ktor

import com.fasterxml.jackson.databind.*
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStarting
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.BaseApplicationEngine
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.EngineSSLConnectorBuilder
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.sslConnector
import java.security.KeyStore
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import org.slf4j.LoggerFactory

fun <K, R> (K.() -> R).andThen(block: K.() -> R): K.() -> R = k@{
	this@andThen.invoke(this@k)
	this.block()
}

object GRPC {
	private val log = LoggerFactory.getLogger(GRPC::class.java)

	fun embeddedServer(
		vararg services: BindableService,
		connector: EngineConnectorConfig = Connectors.default,
		module: Application.() -> Unit = {},
		configure: GRPCConfiguration.() -> Unit = {},
	): GRPCEngine {
		val newConfigure: GRPCConfiguration.() -> Unit = {
			grpc {
				services.forEach(this::addService)
			}
		}

		val newApplication: Application.() -> Unit = {
			module()
		}

		return GlobalScope.embeddedServer(
			connector = connector,
			module = newApplication,
			configure = { newConfigure.andThen(configure).invoke(this) },
		)
	}

	object Connectors {
		val default = http("localhost", 7777)

		fun https(
			keyStore: KeyStore,
			keyAlias: String,
			keyStorePassword: () -> CharArray,
			privateKeyPassword: () -> CharArray,
			builder: EngineSSLConnectorBuilder.() -> Unit = {},
		) = applicationEngineEnvironment {
			sslConnector(
				keyStore,
				keyAlias,
				keyStorePassword,
				privateKeyPassword,
				builder
			)
		}.connectors.first()

		fun http(host: String, port: Int) =
			applicationEngineEnvironment {
				connector {
					this.port = port
					this.host = host
				}
			}.connectors.first()
	}

	fun CoroutineScope.embeddedServer(
		parentCoroutineContext: CoroutineContext = EmptyCoroutineContext,
		connector: EngineConnectorConfig = Connectors.default,
		module: Application.() -> Unit = {},
		configure: GRPCConfiguration.() -> Unit = {},
	): GRPCEngine {
		val environment = applicationEngineEnvironment {
			this.log = LoggerFactory.getLogger("ktor.application")
			this.parentCoroutineContext = coroutineContext + parentCoroutineContext

			connectors.add(connector)
			module(module)
		}
		return GRPCEngineFactory.create(configure = configure, environment = environment)
	}
}

object GRPCEngineFactory : ApplicationEngineFactory<GRPCEngine, GRPCConfiguration> {
	override fun create(
		environment: ApplicationEngineEnvironment,
		configure: GRPCConfiguration.() -> Unit,
	) = GRPCEngine(environment, GRPCConfiguration().apply(configure).build())
}

class GRPCConfiguration : BaseApplicationEngine.Configuration() {
	data class Settings(
		val serverBlocks: List<ServerBuilder<*>.() -> Unit> = emptyList(),
	)

	private var settings = Settings()

	fun grpc(block: ServerBuilder<*>.() -> Unit) {
		settings = settings.copy(serverBlocks = settings.serverBlocks + block)
	}

	fun build(): Settings {
		return settings
	}
}

class GRPCEngine(
	environment: ApplicationEngineEnvironment,
	private val settings: GRPCConfiguration.Settings,
) : BaseApplicationEngine(environment) {

	private lateinit var server: Server

	private val log = LoggerFactory.getLogger("grpc-engine")

	override fun start(wait: Boolean): ApplicationEngine {
		// Build up the grpc configuration.
		server = ServerBuilder
			.forPort(environment.connectors.first().port)
			.apply { settings.serverBlocks.forEach { it(this) } }
			.build()

		// Start it all up!
		environment.monitor.raise(ApplicationStarting, application)
		server.start()
		environment.monitor.raise(ApplicationStarted, application)
		if (wait) {
			server.awaitTermination()
		}
		return this
	}

	override fun stop(gracePeriodMillis: Long, timeoutMillis: Long) {
		environment.monitor.raise(ApplicationStopPreparing, environment)
		environment.monitor.raise(ApplicationStopping, application)
		server.shutdownNow()
		server.awaitTermination(gracePeriodMillis, MILLISECONDS)
		environment.monitor.raise(ApplicationStopped, application)
	}
}

