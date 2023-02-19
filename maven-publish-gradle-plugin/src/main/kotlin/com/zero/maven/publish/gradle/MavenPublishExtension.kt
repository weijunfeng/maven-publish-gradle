package com.zero.maven.publish.gradle

import org.gradle.api.provider.Property

interface MavenPublishExtension {

    /**
     * 认证名
     */
    var mavenUsername: String

    /**
     * 认证密码
     */
    var mavenPassword: String

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
    var publishGroupId: String

    /**
     * 发布版本前缀
     */
    var publishVersionPrefix: String

    /**
     * 任务分组
     */
    val taskGroup: Property<String>
}