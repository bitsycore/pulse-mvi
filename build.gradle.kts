plugins {
//	alias(libs.plugins.android.application) apply false
//	alias(libs.plugins.android.library) apply false
//	alias(libs.plugins.kotlin.android) apply false
	alias(libs.plugins.kotlin.compose) apply false
	alias(libs.plugins.kotlin.multiplatform) apply false
//    alias(libs.plugins.compose.multiplatform) apply false
	alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

extra["compileSdk"] = 36
extra["targetSdk"] = 36
extra["minSdk"] = 24
extra["javaVersion"] = JavaVersion.VERSION_21