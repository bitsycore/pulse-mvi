import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.kotlin.multiplatform.library)
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

val javaVersion: JavaVersion by rootProject.extra

kotlin {

	androidLibrary {
		compileSdk = rootProject.extra["compileSdk"] as Int
		minSdk = rootProject.extra["minSdk"] as Int
		namespace = "com.bitsycore.lib.pulse"
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

	sourceSets {
		all {
			languageSettings.optIn("kotlin.time.ExperimentalTime")
			compilerOptions {
				freeCompilerArgs.add("-Xexpect-actual-classes")
				freeCompilerArgs.add("-Xcontext-parameters")
				freeCompilerArgs.add("-Xcontext-sensitive-resolution")
				freeCompilerArgs.add("-Xexplicit-backing-fields")
			}
		}

		commonMain.dependencies {
			// ====================
			// KotlinX
			implementation(libs.kotlinx.coroutines.core)
			
			// ====================
			// Compose
			implementation(libs.jetbrains.compose.runtime)
			implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
			implementation(libs.jetbrains.androidx.lifecycle.runtime.compose)

		}
	}
}