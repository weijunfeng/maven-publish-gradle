package com.zero.maven.publish.gradle

import org.gradle.api.provider.Property

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
     * 发布版本前缀
     */
    val publishVersionPrefix: Property<String>

    /**
     * 任务分组
     */
    val taskGroup: Property<String>
}