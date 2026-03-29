import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.kotlin.multiplatform.library)
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.serialization)
}

apply(from = rootProject.file("gradle/publish.gradle.kts"))

val javaVersion: JavaVersion by rootProject.extra

kotlin {

	// ================================
	// MARK: Android
	// ================================

	android {
		compileSdk = rootProject.extra["compileSdk"] as Int
		minSdk = rootProject.extra["minSdk"] as Int
		namespace = "com.bitsycore.lib.pulse.savedstate"
		compilerOptions {
			jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
		}
	}

	// ================================
	// MARK: JVM
	// ================================

	jvm {
		compilerOptions {
			jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
		}
	}

	// ================================
	// MARK: Native
	// ================================

	iosArm64()
	iosSimulatorArm64()
	iosX64()

	// ================================
	// MARK: Web
	// ================================

	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		browser()
		nodejs()
	}

	js {
		browser()
		nodejs()
	}

	// ================================
	// MARK: Dependencies
	// ================================

	sourceSets {
		commonMain.dependencies {
			api(projects.pulseViewmodel)
			implementation(libs.kotlinx.serialization.json)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.savedstate)
		}
	}
}
