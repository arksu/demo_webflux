import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.Logging


plugins {
    val kotlinVersion = "1.8.21"

//    java
    id("org.springframework.boot") version "3.0.5"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.flywaydb.flyway") version "9.8.1"
    id("nu.studer.jooq") version "8.2"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
val jooqVersion by extra("3.18.3")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework:spring-jdbc")

    jooqGenerator("org.postgresql:postgresql")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

flyway {
    url = "jdbc:postgresql://localhost/demo_webflux"
    schemas = arrayOf("demo_webflux")
    user = "demo_webflux"
    password = "demo_webflux"
}

buildscript {
    configurations["classpath"].resolutionStrategy.eachDependency {
        if (requested.group == "org.jooq") {
            useVersion("3.18.3")
        }
    }
}

jooq {
    configurations {
        create("main") {  // name of the jOOQ configuration
            generateSchemaSourceOnCompilation.set(true)  // default (can be omitted)

            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/demo_webflux"
                    user = "demo_webflux"
                    password = "demo_webflux"
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "demo_webflux"
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isFluentSetters = true
                        isPojos = true
                        isInterfaces = true

                    }
                    target.apply {
                        packageName = "com.example.jooq"
                        directory = "${project.buildDir}/generated/jooq/main"  // default (can be omitted)
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}
