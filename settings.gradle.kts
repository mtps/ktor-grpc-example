/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 * 
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user guide at https://docs.gradle.org/4.10.2/userguide/multi_project_builds.html
 */

rootProject.name = "ktor-grpc-sample"

pluginManagement {
	val kotlinVersion: String by settings  // defined in gradle.properties
	plugins {
		id("org.jetbrains.kotlin.jvm").version(kotlinVersion)
		id("org.jetbrains.kotlin.kapt").version(kotlinVersion)
	}
}