plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
    `maven-publish`
    `zero-maven-publish`
    `kotlin-dsl`
}

val copyVersionTemplate by tasks.registering(Copy::class) {
    inputs.property("version", version)
    from(layout.projectDirectory.file("Version.kt"))
    into(layout.buildDirectory.dir("generated/mavenpublish-version/main"))
    expand("version" to "$version")
    filteringCharset = "UTF-8"
}

tasks.compileKotlin {
    dependsOn(copyVersionTemplate)
}

sourceSets {
    main {
        java.srcDir("$buildDir/generated/mavenpublish-version/main")
    }
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.21")
    implementation("com.android.tools.build:gradle:7.0.4")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

gradlePlugin {
    plugins {
        create("kmmMavenPublish") {
            id = "io.github.weijunfeng.maven.publish.kmm"//必须为Sonatype注册id开头
            displayName = "KmmMavenPublishPlugin"
            implementationClass = "com.zero.maven.publish.gradle.kmm.KmmMavenPublishPlugin"
        }
        create("androidMavenPublish") {
            id = "io.github.weijunfeng.maven.publish.android"
            displayName = "AndroidMavenPublishPlugin"
            implementationClass = "com.zero.maven.publish.gradle.android.AndroidMavenPublishPlugin"
        }
    }
}

enum class PublicationType {
    SNAPSHOT,
    RELEASE;
}

val publicationVersion = "1.0.1"

publishing {
    repositories {
//        // 发布到远程仓库，不指定url，后面根据具体的发布动态设置远程url
//        maven {
//            credentials {
//                // 远程认证信息
//                username = mavenUserName
//                password = mavenPassword
//            }
//            // 允许使用http链接发布
//            isAllowInsecureProtocol = true
//            url = uri(mavenUrl)
//        }
        // 发布到本地
        mavenLocal {
            name = "local"
            url = uri("${project.buildDir}/repo")
        }
    }
    publications.withType<MavenPublication> {
        artifact(sourcesJar)
        version = publicationVersion
        groupId = "io.github.weijunfeng" // 必须为Sonatype注册id
        artifactId = "mavenPublish-gradle-plugin"
    }
}

afterEvaluate {
    tasks.withType<PublishToMavenRepository> {
        group = "buildapk"
    }
}
