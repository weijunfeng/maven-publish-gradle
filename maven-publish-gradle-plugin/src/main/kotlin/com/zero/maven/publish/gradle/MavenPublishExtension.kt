package com.zero.maven.publish.gradle

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

interface MavenPublishExtension {
    /**
     * 设置发布的版本
     */
    val publishVersion: Property<String>

    /**
     * 设置发布的 groupId
     */
    val publishGroupId: Property<String>

    /**
     * 添加一个在本地 Maven url地址
     */
    val mavenLocalUrl: Property<String>

    /**
     * 设置对存储库进行身份验证时要使用的用户名。
     */
    val mavenUserName: Property<String>

    /**
     * 设置对存储库进行身份验证时使用的密码。
     */
    val mavenPassword: Property<String>

    /**
     * 任务分组
     */
    val taskGroup: Property<String>

    val groupDefaultLocal: Property<Boolean>

    /**
     * 发布类型
     */
    val publications: MapProperty<String, PublicationType>

    fun createPublication(name: String, action: PublicationType.() -> Unit) {
        publications.put(name, PublicationType(name).apply(action))
    }
}

val MavenPublishExtension.publicationTypes
    get() = publications.get().values