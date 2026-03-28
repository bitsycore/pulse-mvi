import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.kotlin.multiplatform.library)
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.serialization)
}

val javaVersion: JavaVersion by rootProject.extra

kotlin {

	androidLibrary {
		compileSdk = rootProject.extra["compileSdk"] as Int
		minSdk = rootProject.extra["minSdk"] as Int
		namespace = "com.bitsycore.lib.pulse.savedstate"
		compilerOptions {
			jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
		}
	}

	jvm {
		compilerOptions {
			jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
		}
	}

	iosArm64()
	iosSimulatorArm64()
	iosX64()

	sourceSets {
		commonMain.dependencies {
			api(project(":pulse-viewmodel"))
			implementation(libs.kotlinx.serialization.json)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.savedstate)
		}
	}
}
