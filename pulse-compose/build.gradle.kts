import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.kotlin.multiplatform.library)
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.compose.multiplatform)
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
		namespace = "com.bitsycore.lib.pulse.compose"
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

	sourceSets {
		commonMain.dependencies {
			api(projects.pulse)
			implementation(libs.jetbrains.compose.runtime)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
			implementation(libs.jetbrains.androidx.lifecycle.runtime.compose)
		}
	}
}
