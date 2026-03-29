import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.kotlin.multiplatform.library)
	alias(libs.plugins.kotlin.multiplatform)
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
		namespace = "com.bitsycore.lib.pulse.viewmodel"
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
			api(projects.pulse)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
		}
	}
}
