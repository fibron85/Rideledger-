// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

tasks.register("copyDebugApkToRoot") {
  dependsOn(":app:assembleDebug")
  val sourceApk = layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk").asFile
  val destApk = layout.projectDirectory.file("app-debug.apk").asFile
  inputs.file(sourceApk)
  outputs.file(destApk)
  doLast {
    sourceApk.copyTo(destApk, overwrite = true)
  }
}
