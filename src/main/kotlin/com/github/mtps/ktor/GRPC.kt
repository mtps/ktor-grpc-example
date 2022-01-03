package com.github.mtps.ktor

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.ktor.application.Application
import io.ktor.application.ApplicationStarted
import io.ktor.application.ApplicationStarting
import io.ktor.application.ApplicationStopPreparing
import io.ktor.application.ApplicationStopped
import io.ktor.application.ApplicationStopping
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.BaseApplicationEngine
import io.ktor.server.engine.EngineAPI
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.EngineSSLConnectorBuilder
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.sslConnector
import java.security.KeyStore
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.GlobalScope.coroutineContext
import org.slf4j.LoggerFactory

fun <K, R> (K.() -> R).andThen(block: K.() -> R): K.() -> R = k@{
	this@andThen.invoke(this@k)
	this.block()
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

object GRPC {
	fun embeddedServer(
		services: List<BindableService>,
		connector: EngineConnectorConfig = Connectors.default,
		module: Application.() -> Unit = {},
		configure: GRPCConfiguration.() -> Unit = {},
	): GRPCEngine {
		val newConfigure: GRPCConfiguration.() -> Unit = {
			grpc { services.forEach(::addService) }
		}

		return embeddedKtorServer(
			connector = connector,
			module = module,
			configure = { newConfigure.andThen(configure).invoke(this) },
		)
	}

	private fun embeddedKtorServer(
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
	): GRPCEngine {
		return GRPCEngine(environment, GRPCConfiguration().apply(configure).build())
	}
}

@OptIn(EngineAPI::class)
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

@OptIn(EngineAPI::class)
class GRPCEngine(
	environment: ApplicationEngineEnvironment,
	private val settings: GRPCConfiguration.Settings,
) : BaseApplicationEngine(environment) {

	private lateinit var server: Server

	override fun start(wait: Boolean): ApplicationEngine {
		val port = environment.connectors.first().port
		// Build up the grpc configuration.
		server = ServerBuilder
			.forPort(port)
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

