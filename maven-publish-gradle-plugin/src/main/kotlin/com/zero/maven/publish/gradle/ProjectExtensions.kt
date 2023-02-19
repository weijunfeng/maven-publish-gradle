package com.zero.maven.publish.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.withType


internal inline val Project.gradlePublishing: PublishingExtension
    get() = extensions.getByType(PublishingExtension::class.java)

internal inline val Project.mavenPublishExtension: MavenPublishExtension
    get() = extensions.getByType(MavenPublishExtension::class.java)

internal fun Project.gradlePublishing(action: Action<PublishingExtension>) {
    gradlePublishing.apply {
        action.execute(this)
    }
}

internal inline val Project.androidComponents: AndroidComponentsExtension<*, *, *>
    get() = extensions.getByType(AndroidComponentsExtension::class.java)

fun Project.doLastPrintUrl() {
    tasks.withType<AbstractPublishToMaven>().forEach {
        if (it !is PublishToMavenRepository) {
            return@forEach
        }
        val publication = it.publication
        if (publication !is DefaultMavenPublication) {
            return@forEach
        }
        it.doLast {
            val repositoryUrl = it.repository.url
            var url = repositoryUrl.toString()
            url = url.replace("repository/android-snapshot/", "#browse/browse:android-snapshot:")
            url = url.replace("repository/android-release/", "#browse/browse:android-release:")
            url = url.replace("repository/android-beta/", "#browse/browse:android-beta:")
            println(
                "文件位置: ${url}${
                    publication.groupId.replace(
                        Regex("\\."),
                        "/"
                    )
                }/${publication.artifactId}/${publication.version}"
            )
            println(
                """添加依赖仓库:
                        |maven{
                        |   setUrl("$repositoryUrl")
                        |}
                    """.trimMargin()
            )
            println("构建远程依赖结束, 在依赖项目的build.gradle中dependencies添加如下内容")
            println("implementation '${publication.groupId}:${publication.artifactId}:${publication.version}'")
        }
    }
}

fun RepositoryHandler.remoteMaven(project: Project, remoteName: String, remoteUrl: String?) {
    if (remoteUrl.isNullOrEmpty()) {
        return
    }
    val mavenPublishExtension = project.mavenPublishExtension
    maven {
        name = remoteName
        credentials {
            // 远程认证信息
            username = mavenPublishExtension.mavenUsername
            password = mavenPublishExtension.mavenPassword
        }
        // 允许使用http链接发布
        isAllowInsecureProtocol = true
        url = project.uri(remoteUrl)
    }
}

fun Project.setRepositories() {
    val mavenPublishExtension = project.mavenPublishExtension
    gradlePublishing {
        repositories {
            remoteMaven(this@setRepositories, "snapshot", mavenPublishExtension.mavenSnapshotUrl)
            remoteMaven(this@setRepositories, "release", mavenPublishExtension.mavenReleaseUrl)
            remoteMaven(this@setRepositories, "bate", mavenPublishExtension.mavenBateUrl)
            // 发布到本地
            maven {
                name = "local"
                url = project.uri("./build/repo/")
            }
        }
    }
}

fun Project.setGroupId() {
    val startParameterTaskNames = gradle.startParameter.taskNames
    val startParameterTaskName = if (startParameterTaskNames.size != 1) {
        ""
    } else {
        startParameterTaskNames[0]
    }
    val startParameterTask = tasks.findByName(startParameterTaskName)
    var repository: MavenArtifactRepository? = null
    if (startParameterTask is PublishToMavenRepository) {
        repository = startParameterTask.repository
    } else if (startParameterTask is CombinedPublishToMavenTask) {
        repository = startParameterTask.repository
    }
    val mavenPublishExtension = project.mavenPublishExtension
    gradlePublishing.publications.withType<MavenPublication> {
        groupId = mavenPublishExtension.publishGroupId
        version = "${mavenPublishExtension.publishVersionPrefix}${
            if (repository != null) {
                "-${repository.name.toUpperCase()}"
            } else {
                ""
            }
        }"
    }
}