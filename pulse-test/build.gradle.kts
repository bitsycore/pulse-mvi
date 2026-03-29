import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.android.kotlin.multiplatform.library)
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
		namespace = "com.bitsycore.lib.pulse.test"
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

	// macOS
	macosArm64()
	// iOS
	watchosArm64()
	watchosSimulatorArm64()
	tvosArm64()
	tvosSimulatorArm64()
	iosArm64()
	iosSimulatorArm64()
	iosX64()
	// Linux
	linuxArm64()
	linuxX64()
	// Windows
	mingwX64()
	// Android
	androidNativeX64()
	androidNativeX86()
	androidNativeArm32()
	androidNativeArm64()

	// ================================
	// MARK: Web
	// ================================

	js {
		browser()
		nodejs()
	}
	@Suppress("OPT_IN_USAGE")
	wasmJs {
		browser()
		nodejs()
	}
	@Suppress("OPT_IN_USAGE")
	wasmWasi {
		nodejs()
	}

	sourceSets {
		commonMain.dependencies {
			api(projects.pulse)
			implementation(libs.kotlin.test)
			implementation(libs.kotlinx.coroutines.test)
		}
	}
}
