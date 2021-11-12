package com.github.mtps.ktor

import com.google.protobuf.empty
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
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
import io.ktor.server.engine.embeddedServer as ktorServer
import java.util.concurrent.TimeUnit.MILLISECONDS

object GRPC {
    fun embeddedServer(
        vararg services: BindableService,
        module: Application.() -> Unit = {},
        configure: GRPCConfiguration.() -> Unit = {},
    ): GRPCEngine {
        val newConfigure: GRPCConfiguration.() -> Unit = {
            grpc { services.forEach(this::addService) }
            configure()
        }
        return embeddedServer(module, newConfigure)
    }

    fun embeddedServer(
        module: Application.() -> Unit = {},
        configure: GRPCConfiguration.() -> Unit = {},
    ) = ktorServer(GRPCEngineFactory, configure = configure, module = module)
}

object GRPCEngineFactory : ApplicationEngineFactory<GRPCEngine, GRPCConfiguration> {
    override fun create(
        environment: ApplicationEngineEnvironment,
        configure: GRPCConfiguration.() -> Unit
    ): GRPCEngine = GRPCEngine(environment, configure)
}

@OptIn(EngineAPI::class)
class GRPCConfiguration : BaseApplicationEngine.Configuration() {
    data class Settings(
        val port: Int = 7777,
        val reflection: Boolean = true,
        val server: ServerBuilder<*>.() -> Unit = {}
    )

    private var settings = Settings()

    fun grpc(block: ServerBuilder<*>.() -> Unit) {
        settings = settings.copy(server = block)
    }

    var port
        get() = settings.port
        set(value) {
            settings = settings.copy(port = value)
        }

    var reflection
        get() = settings.reflection
        set(value) {
            settings = settings.copy(reflection = value)
        }

    fun build(): Settings {
        return settings
    }
}

@OptIn(EngineAPI::class)
class GRPCEngine(
    environment: ApplicationEngineEnvironment,
    configure: GRPCConfiguration.() -> Unit
) : BaseApplicationEngine(environment) {

    private val cfg = GRPCConfiguration().apply(configure).build()
    private lateinit var server: Server

    override fun start(wait: Boolean): ApplicationEngine {
        // Build the base configuration.
        val s = ServerBuilder.forPort(cfg.port)
        if (cfg.reflection) {
            s.addService(ProtoReflectionService.newInstance())
        }

        // Run user configuration last to allow overrides.
        cfg.server(s)

        // Build the grpc server.
        server = s.build()

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
        server.awaitTermination(gracePeriodMillis, MILLISECONDS)
        server.shutdownNow()
        environment.monitor.raise(ApplicationStopped, application)
    }
}

