plugins {
    `maven-publish`
    signing
}

ext["signing.keyId"] = null
ext["signing.password"] = null
ext["signing.secretKey"] = null
ext["signing.secretKeyRingFile"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null
ext["gradle.publish.key"] = null
ext["gradle.publish.secret"] = null
val localPropsFile = project.rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.reader()
        .use { java.util.Properties().apply { load(it) } }
        .onEach { (name, value) -> ext[name.toString()] = value }
} else {
    ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.secretKey"] = System.getenv("SIGNING_SECRET_KEY")
    ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
    ext["gradle.publish.key"] = System.getenv("GRADLE_PUBLISH_KEY")
    ext["gradle.publish.secret"] = System.getenv("GRADLE_PUBLISH_SECRET")
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

fun getExtraString(name: String) = ext[name]?.toString()
publishing {
    repositories {
        maven {
            name = "release"
            setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
        maven {
            name = "snapshot"
            setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials {
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
    }

    publications.withType<MavenPublication> {
        artifact(emptyJavadocJar.get())

        pom {
            //组件的基本信息
            name.set("maven-publish-gradle")
            description.set("maven-publish-gradle")
            url.set("https://github.com/weijunfeng/maven-publish-gradle")
            //licenses文件
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            //开发者信息
            developers {
                developer {
//                    id.set("weijunfeng")
                    name.set("weijunfeng")
                    email.set("891130789@qq.com")
                }
            }
            //版本控制仓库地址
            scm {
                url.set("https://github.com/weijunfeng/maven-publish-gradle")
            }
        }
    }
}

getExtraString("signing.keyId")?.let { keyId ->
    signing {
        getExtraString("signing.secretKey")?.let { secretKey ->
            useInMemoryPgpKeys(keyId, secretKey, getExtraString("signing.password"))
        }
        sign(publishing.publications)
    }
}