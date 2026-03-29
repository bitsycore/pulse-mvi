import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
	alias(libs.plugins.kotlin.compose) apply false
	alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
	alias(libs.plugins.android.kotlin.multiplatform.library) apply false
	alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
}

extra["compileSdk"] = 36
extra["targetSdk"] = 36
extra["minSdk"] = 23
extra["javaVersion"] = JavaVersion.VERSION_11

// Pulse version management
val baseVersion = providers.gradleProperty("pulse.version").get() as String
val isSnapshot = providers.gradleProperty("pulse.snapshot").map { it.toBoolean() }.get() as Boolean
val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
val pulseVersion = if (isSnapshot) "$baseVersion-SNAPSHOT-$timestamp" else baseVersion

extra["pulseVersion"] = pulseVersion