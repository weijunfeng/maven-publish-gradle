package com.zero.maven.publish.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.zero.maven.publish.gradle.kmm.PublishAllIosTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.withType


internal inline val Project.gradlePublishing: PublishingExtension
    get() = extensions.getByType(PublishingExtension::class.java)

internal fun Project.gradlePublishing(action: Action<PublishingExtension>) {
    gradlePublishing.apply {
        action.execute(this)
    }
}

internal inline val Project.androidComponents: AndroidComponentsExtension<*, *, *>
    get() = extensions.getByType(AndroidComponentsExtension::class.java)

internal fun Project.initRepositoryUrl(publications: PublicationContainer, mavenPublishExtension: MavenPublishExtension) {
    val startParameterTaskNames = this@initRepositoryUrl.gradle.startParameter.taskNames
    val startParameterTaskName = if (startParameterTaskNames.size != 1) {
        ""
    } else {
        startParameterTaskNames[0]
    }
    val startParameterTask = this@initRepositoryUrl.tasks.findByName(startParameterTaskName)
    var startPublicationType: PublicationType? = null
    var repository: MavenArtifactRepository? = null
    var publication: MavenPublication? = null
    if (startParameterTask is PublishToMavenRepository) {
        publication = startParameterTask.publication
        startPublicationType = publication.publicationType(mavenPublishExtension)
        repository = startParameterTask.repository
    } else if (startParameterTask is PublishAllIosTask) {
        startPublicationType = startParameterTask.publicationType
        publication = publications.filterIsInstance<MavenPublication>()
            .firstOrNull {
                startPublicationType == it.publicationType(mavenPublishExtension)
            }
        repository = startParameterTask.repository
    }
    if (repository != null
        && startPublicationType != null
        && publication != null
        && startPublicationType.url != repository.url?.toString()
    ) {
        // 根据发布版本信息，动态指定发布到的仓库地址
        repository.url = this@initRepositoryUrl.uri(startPublicationType.url)
    }
}

internal fun RepositoryHandler.publishRepository(project: Project, mavenPublishExtension: MavenPublishExtension) {
    this.apply {
        // 发布到远程仓库，不指定url，后面根据具体的发布动态设置远程url
        maven {
            credentials {
                // 远程认证信息
                username = mavenPublishExtension.mavenUserName.get()
                password = mavenPublishExtension.mavenPassword.get()
            }
            // 允许使用http链接发布
            isAllowInsecureProtocol = true
        }
        // 发布到本地
        if (mavenPublishExtension.mavenLocalUrl.get().isNotEmpty()) {
            mavenLocal {
                url = project.uri(mavenPublishExtension.mavenLocalUrl)
            }
        }
    }
}

internal fun Project.groupPublishToMavenTask(mavenPublishExtension: MavenPublishExtension) {
    tasks.withType<AbstractPublishToMaven>().forEach {
        if (!mavenPublishExtension.groupDefaultLocal.get() && it !is PublishToMavenRepository) {
            return@forEach
        }
        val publication = it.publication
        publication.publicationType(mavenPublishExtension) ?: return@forEach
        it.group = mavenPublishExtension.taskGroup.get()
    }
}

internal fun Project.doLastPrintUrl(mavenPublishExtension: MavenPublishExtension) {
    tasks.withType<PublishToMavenRepository>().forEach {
        val publication = it.publication
        publication.publicationType(mavenPublishExtension) ?: return@forEach
        it.doLast {
            var url = it.repository.url.toString()
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
            println("构建远程依赖结束, 在依赖项目的build.gradle中dependencies添加如下内容")
            println("implementation '${publication.groupId}:${publication.artifactId}:${publication.version}'")
        }
    }
}

fun MavenPublication.publicationType(mavenPublishExtension: MavenPublishExtension): PublicationType? {
    if (this !is DefaultMavenPublication) {
        return null
    }
    val component = this.component ?: return null
    mavenPublishExtension.publicationTypes.forEach {
        if (this@publicationType.version == mavenPublishExtension.publicationVersion(it)
            && this@publicationType.name == component.publicationName(it)
        ) {
            return it
        }
    }
    return null
}

fun SoftwareComponent.publicationName(publicationType: PublicationType): String {
    return "${this.name.toLowerCase().capitalize()}${publicationType.typeName}"
}

fun MavenPublishExtension.publicationVersion(publicationType: PublicationType): String {
    return this.publishVersion.get() +
            if (publicationType.typeName.startsWith("-")) {
                publicationType.typeName
            } else {
                "-" + publicationType.typeName
            }
}
