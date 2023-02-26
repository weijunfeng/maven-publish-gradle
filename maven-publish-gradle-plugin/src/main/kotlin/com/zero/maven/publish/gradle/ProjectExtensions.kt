package com.zero.maven.publish.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.withType
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


internal inline val Project.gradlePublishing: PublishingExtension
    get() = extensions.getByType(PublishingExtension::class.java)

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

fun RepositoryHandler.configRemoteMaven(
    project: Project,
    remoteName: String,
    mavenPublishExtension: MavenPublishExtension,
    remoteUrl: String?
) {
    if (remoteUrl.isNullOrEmpty()) {
        return
    }
    maven {
        name = remoteName
        credentials {
            // 远程认证信息
            username = mavenPublishExtension.mavenUsername.get()
            password = mavenPublishExtension.mavenPassword.get()
        }
        // 允许使用http链接发布
        isAllowInsecureProtocol = true
        url = project.uri(remoteUrl)
    }
}

fun Project.setRepositories(mavenPublishExtension: MavenPublishExtension) {
    gradlePublishing {
        repositories {
            configRemoteMaven(
                this@setRepositories,
                "snapshot", mavenPublishExtension, mavenPublishExtension.mavenSnapshotUrl
            )
            configRemoteMaven(
                this@setRepositories,
                "release", mavenPublishExtension, mavenPublishExtension.mavenReleaseUrl
            )
            configRemoteMaven(
                this@setRepositories,
                "bate", mavenPublishExtension, mavenPublishExtension.mavenBateUrl
            )
            // 发布到本地
            maven {
                name = "local"
                url = project.uri("./build/repo/")
            }
        }
    }
}

fun Project.setGroupId(mavenPublishExtension: MavenPublishExtension) {
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
    gradlePublishing.publications.withType<MavenPublication> {
        groupId = mavenPublishExtension.publishGroupId.get()
        var publishVersion = mavenPublishExtension.publishVersion.get()
        if (repository != null) {
            val upperCaseRepositoryName = repository.name.toUpperCase(Locale.ROOT)
            if (!publishVersion.endsWith(upperCaseRepositoryName)) {
                publishVersion += "-${upperCaseRepositoryName}"
            }
        }
        version = publishVersion
    }
}

/**
 * 在android插件执行后执行[fn]
 */
internal fun <T> Project.afterAndroidPluginEvaluated(fn: Project.() -> T) {
    if (state.executed) {
        afterEvaluate {
            fn()
        }
        return
    }

    /** If there's already an Android plugin applied, just dispatch the action to `afterEvaluate`, it gets executed after AGP's actions */
    if (androidPluginIds.any { pluginManager.hasPlugin(it) }) {
        afterEvaluate { fn() }
        return
    }

    val isDispatchedAfterAndroid = AtomicBoolean(false)

    /**
     * This queue holds all actions submitted to `whenEvaluated` in this project, waiting for one of the Android plugins to be applied.
     * After (and if) an Android plugin gets applied, we dispatch all the actions in the queue to `afterEvaluate`, so that they are
     * executed after what AGP scheduled to `afterEvaluate`. There are different Android plugins, so actions in the queue also need to check
     * if it's the first Android plugin, using `isDispatched` (each has its own instance).
     */
    val afterAndroidDispatchQueue = project.extensions.extraProperties.getOrPut("org.jetbrains.kotlin.whenEvaluated") {
        val queue = mutableListOf<() -> Unit>()
        // Trigger the actions on any plugin applied; the actions themselves ensure that they only dispatch the fn once.
        androidPluginIds.forEach { id ->
            pluginManager.withPlugin(id) { queue.forEach { it() } }
        }
        queue
    }
    afterAndroidDispatchQueue.add {
        if (!isDispatchedAfterAndroid.getAndSet(true)) {
            afterEvaluate { fn() }
        }
    }

    afterEvaluate {
        /** If no Android plugin was loaded, then the action was not dispatched, and we can freely execute it now */
        if (!isDispatchedAfterAndroid.getAndSet(true)) {
            fn()
        }
    }
}

internal inline fun <reified T : Any> ExtraPropertiesExtension.getOrPut(key: String, provideValue: () -> T): T {
    return synchronized(this) {
        if (!has(key)) {
            set(key, provideValue())
        }
        get(key) as T
    }
}