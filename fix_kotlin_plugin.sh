sed -i '/kotlin-compose = /i kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }' gradle/libs.versions.toml
sed -i '/alias(libs.plugins.android.application)/a \    alias(libs.plugins.kotlin.android)' app/build.gradle.kts
