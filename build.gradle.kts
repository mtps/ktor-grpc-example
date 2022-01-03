import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

extra["protocVersion"] = "3.19.1"
extra["grpcVersion"] = "1.42.1"
extra["grpcKotlinVersion"] = "1.2.0"
extra["koinVersion"] = "3.1.4"
extra["ktorVersion"] = "1.6.7"

plugins {
    kotlin("jvm")

    id("com.google.protobuf").version("0.8.18")

    java
    idea
    application
}

repositories {
    mavenCentral()
}

group = "com.github.mtps.ktor"
version = "1.0-SNAPSHOT"

val javaTarget = JavaVersion.VERSION_11
java.sourceCompatibility = javaTarget

sourceSets.main {
    java {
        srcDirs(
            "build/generated/source/proto/main/java",
            "build/generated/source/proto/main/grpc",
            "build/generated/source/proto/main/grpckt",
            "build/generated/source/proto/main/kotlin",
        )
    }
}

val kotlinJvmOptions: KotlinJvmOptions.() -> Unit = {
    freeCompilerArgs = listOf("-Xjsr305=strict", "-Xopt-in=kotlin.RequiresOptIn")
    jvmTarget = javaTarget.majorVersion
}

tasks.compileKotlin {
    kotlinOptions(kotlinJvmOptions)
}

tasks.compileTestKotlin {
    kotlinOptions(kotlinJvmOptions)
}

//kotlin { experimental { coroutines "enable" } }
//

application {
    mainClass.set("com.github.mtps.ktor.MainKt")
}

val ktorVersion: String by extra
val koinVersion: String by extra
val grpcVersion: String by extra
val grpcKotlinVersion: String by extra
val protocVersion: String by extra


dependencies {
    // HTTP
    implementation("io.ktor:ktor-features:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")

    // DI
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    testImplementation("junit:junit:4.13.2")

    // R2DBC - (async jdbc)
    implementation("com.github.jasync-sql:jasync-postgresql:2.0.4")

    // GRPC
    implementation("io.grpc:grpc-api:$grpcVersion")
    implementation("io.grpc:grpc-netty:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("io.grpc:grpc-services:$grpcVersion")

    // Protobuf
    implementation("com.google.protobuf:protobuf-java-util:$protocVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protocVersion")

    // ~~ Test ~~
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("junit:junit:4.13.2")
}



protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protocVersion"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk7@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}
