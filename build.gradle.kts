plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.22"
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    id("io.micronaut.application") version "4.3.2"
}

version = "0.1"
group = "com.jpy.wordbook"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Micronaut core
    ksp("io.micronaut:micronaut-http-validation")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.yaml:snakeyaml")

    // Views - Thymeleaf
    implementation("io.micronaut.views:micronaut-views-thymeleaf")

    // Database - SQLite with JDBC
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    runtimeOnly("org.xerial:sqlite-jdbc:3.44.1.0")

    // Flyway for migrations
    implementation("io.micronaut.flyway:micronaut-flyway")
    runtimeOnly("org.flywaydb:flyway-core")

    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Japanese text processing (furigana, romaji)
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")

    // Testing
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

application {
    mainClass.set("com.jpy.wordbook.ApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.jpy.wordbook.*")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
