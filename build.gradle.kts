import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

project.version = "0.1.0"

plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.openapi.generator") version "6.6.0"
}

buildscript {
    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin:6.6.0")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
apply(plugin = "org.openapi.generator")

val junitVersion = "4.13.2"
val mockitoVersion = "3.12.4"
val powermockVersion = "2.0.9"

val swaggerAnnotationsVersion = "1.5.22"
val jacksonVersion = "2.10.5"
val jacksonDatabindVersion = "2.10.5.1"
val jacksonDatabindNullableVersion = "0.2.2"
val jakartaAnnotationVersion = "1.3.5"
val springWebVersion = "5.3.18"
val jodatimeVersion = "2.9.9"

dependencies {

    implementation("io.swagger:swagger-annotations:$swaggerAnnotationsVersion")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.springframework:spring-web:$springWebVersion")
    implementation("org.springframework:spring-context:$springWebVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonDatabindVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:$jacksonVersion")
    implementation("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:$jacksonVersion")
    implementation("org.openapitools:jackson-databind-nullable:$jacksonDatabindNullableVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.mockito:mockito-inline:$mockitoVersion")
    testImplementation("org.powermock:powermock-core:$powermockVersion")
    testImplementation("org.powermock:powermock-api-mockito2:$powermockVersion")
    testImplementation("org.powermock:powermock-module-junit4:$powermockVersion")
}

val clientOutputDir = "$buildDir/generated/openapi/default/java-client"
val clientArtifactId = project.name.toLowerCase().replace(Regex("[^a-z0-9]"), "")
val clientArtifactVersion = project.version.toString()
println("project.version: ${project.version}")

tasks.register("generate-http-client", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class.java) {
    inputSpec.set("$rootDir/specs/openapi.yml")
    outputDir.set(clientOutputDir)
    generatorName.set("java")
    val packageName = "dev.commandk.javasdk"
    configOptions.set(
        mutableMapOf(
            "basePackage" to packageName,
            "apiPackage" to "$packageName.api",
            "modelPackage" to "$packageName.models",
            "packageName" to packageName,
            "artifactId" to clientArtifactId,
            "artifactVersion" to clientArtifactVersion,
        ),
    )
    additionalProperties.set(
        mutableMapOf("library" to "resttemplate"),
    )
}

tasks.register("build-http-client", org.gradle.api.tasks.Exec::class.java) {
    workingDir = project.rootDir
    commandLine = listOf(
        "./gradlew",
        "-p",
        "$buildDir/generated/openapi/default/java-client",
        ":assemble",
    )
    dependsOn("generate-http-client")
    project.dependencies.add(
        "implementation",
        files(
            "$clientOutputDir/build/libs/" +
                "$clientArtifactId${if (clientArtifactVersion == "unspecified") "" else "-$clientArtifactVersion"}.jar",
        ),
    )
}

tasks.named("compileJava").configure {
    dependsOn("build-http-client")
    dependsOn()
}

tasks.named<ShadowJar>("shadowJar").configure {
    relocate("com.fasterxml", "dev.commandk.com.fasterxml")
    relocate("io.swagger", "dev.commandk.io.swagger")
    relocate("org.openapitools", "dev.commandk.org.openapitools")
    relocate("org.aopalliance", "dev.commandk.org.aopalliance")
    relocate("org.apache", "dev.commandk.org.apache")
    relocate("org.springframework", "dev.commandk.org.springframework")
}

tasks.named("assemble").configure {
    dependsOn("javadoc")
    dependsOn("shadowJar")
}

tasks.withType<Test> {
    useJUnit()
//    if (isJava9OrLater()) {
//        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
//        jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
//        jvmArgs("--add-opens", "java.net.http/java.net.http=ALL-UNNAMED")
//        jvmArgs("--add-opens", "java.net.http/jdk.internal.net.http=ALL-UNNAMED")
//        jvmArgs("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
//    }
}

tasks.withType<Javadoc>() {
    options.overview("src/main/javadoc/overview.html")
}

publishing {
    publications {
        create<MavenPublication>("main") {
            groupId = "dev.commandk"
            artifactId = "javasdk"
            version = System.getenv("VERSION_TAG") ?: version

            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/commandk-dev/commandk")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
