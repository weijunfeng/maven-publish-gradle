package com.zero.maven.publish.gradle

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPom

interface MavenPublishExtension {

    /**
     * 认证名, 当配置[mavenReleaseUrl]或[mavenSnapshotUrl]或[mavenBateUrl]时必须配置该值
     */
    val mavenUsername: Property<String>

    /**
     * 认证密码, 当配置[mavenReleaseUrl]或[mavenSnapshotUrl]或[mavenBateUrl]时必须配置该值
     */
    val mavenPassword: Property<String>

    /**
     * Snapshot版本地址
     */
    var mavenSnapshotUrl: String

    /**
     * Release版地址
     */
    var mavenReleaseUrl: String

    /**
     * 测试版地址
     */
    var mavenBateUrl: String

    /**
     * 发布的 groupId
     */
    val publishGroupId: Property<String>

    /**
     * 发布版本号,如果不以LOCAL或SNAPSHOT或RELEASE结尾 则自动追加其
     */
    val publishVersion: Property<String>

    /**
     * 任务分组
     */
    val taskGroup: Property<String>

    /**
     * 配置将要发布的 POM
     */
    val pom: Property<Action<MavenPom>>
}