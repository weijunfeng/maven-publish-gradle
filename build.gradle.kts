plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version("7.4.1").apply(false)
    id("com.android.library").version("7.4.1").apply(false)
//    id("com.zero.maven.publish").version("1.0.2-SNAPSHOT").apply(false)
    kotlin("android").version("1.6.21").apply(false)
    kotlin("multiplatform").version("1.6.21").apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
