import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.kotlin.multiplatform.library)
	alias(libs.plugins.kotlin.multiplatform)
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

	// iOS
	iosArm64()
	iosSimulatorArm64()
	iosX64()

	// macOS
	macosArm64()

	// Linux
	linuxArm64()
	linuxX64()

	// Windows
	mingwX64 {
		binaries {
			staticLib {

			}
		}
	}

	// watchOS
	watchosArm64()
	watchosSimulatorArm64()

	// tvOS
	tvosArm64()
	tvosSimulatorArm64()

	sourceSets {
		commonMain.dependencies {
			implementation(libs.kotlinx.coroutines.core)
		}
	}
}
