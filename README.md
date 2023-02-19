# maven-publish-gradle
## **一个对maven-publish的快速配置扩展工具**

## 版本
最新版 1.0.1

## **特性**

1. 支持快速配置`maven` 发布配置信息
2. 支持输出依赖配置信息
3. 支持配置发布任务组

## 使用

1. 项目根目录中build.gradle.kts的`plugins` 添加如下信息

```jsx
id("io.github.weijunfeng.maven.publish.kmm").version("1.0.1").apply(false)
```

1. 在需要使用的组件的build.gradle.kts的最后添加引入plugin

```jsx
apply {
    plugin("io.github.weijunfeng.maven.publish.kmm")
}
extensions.configure<com.zero.maven.publish.gradle.MavenPublishExtension>("mavenPublish") {
    mavenUsername = ""

    mavenPassword = ""

    // 配置Snapshot发布地址，启用Snapshot版本发布
    mavenSnapshotUrl = "https://github.com" 

    // 配置Release发布地址，启用Release版本发布
    mavenReleaseUrl = "https://github.com"

    publishGroupId = "com.weijunfeng.maven.plugin"

		// 发布版本前缀，打包时自动添加发布类型["-LOCAL","-SNAPSHOT","-RELEASE"]
    publishVersionPrefix = "1.01.1"
}
```

## 待解决问题

1. 不能在组件的build.gradle.kts中直接使用`id("io.github.weijunfeng.maven.publish.kmm").version("1.0.1")` 引入，会导致需要的task任务无法被找到问题
