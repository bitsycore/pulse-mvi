import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.kotlin.multiplatform.library)
	alias(libs.plugins.kotlin.multiplatform)
}

apply(from = rootProject.file("gradle/publish.gradle.kts"))

val javaVersion = rootProject.extra["javaVersion"] as JavaVersion

kotlin {

	explicitApi()

	// The public API is dumped in api/: checkKotlinAbi fails on a change that was not meant,
	// updateKotlinAbi records one that was.
	@OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
	abiValidation {
		enabled.set(true)
	}

	// ================================
	// MARK: Android
	// ================================

	android {
		compileSdk = rootProject.extra["compileSdk"] as Int
		minSdk = rootProject.extra["minSdk"] as Int
		namespace = "com.bitsycore.lib.pulse"
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

	js {
		browser()
	}

	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		browser()
	}

	// ================================
	// MARK: Dependencies
	// ================================

	sourceSets {
		commonMain.dependencies {
			implementation(libs.kotlinx.coroutines.core)
		}
	}
}
