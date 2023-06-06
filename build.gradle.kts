import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.Logging

plugins {
    val kotlinVersion = "1.8.21" // https://kotlinlang.org/
    val springVersion = "3.0.7" // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version springVersion
    id("io.spring.dependency-management") version "1.1.0" // https://plugins.gradle.org/plugin/io.spring.dependency-management
    id("org.flywaydb.flyway") version "9.18.0" // https://plugins.gradle.org/plugin/org.flywaydb.flyway
    id("nu.studer.jooq") version "8.2.1" // https://plugins.gradle.org/plugin/nu.studer.jooq
}

group = "com.example"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.github.wimdeblauwe:error-handling-spring-boot-starter:4.1.0")

    implementation("org.springframework:spring-jdbc")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.2")
    implementation("org.flywaydb:flyway-core:9.18.0")
    implementation("org.jooq:jooq-kotlin:3.18.4")
    implementation("org.jooq:jooq-kotlin-coroutines:3.18.4")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    jooqGenerator("org.postgresql:postgresql:42.5.4") // https://mvnrepository.com/artifact/org.postgresql/postgresql

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    runtimeOnly("io.r2dbc:r2dbc-pool")

    runtimeOnly(
        group = "io.netty",
        name = "netty-resolver-dns-native-macos",
        classifier = "osx-aarch_64"
    )

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:testcontainers:1.18.1") // https://mvnrepository.com/artifact/org.testcontainers/testcontainers
    testImplementation("org.testcontainers:postgresql:1.18.1") // https://mvnrepository.com/artifact/org.testcontainers/testcontainers
    testImplementation("org.testcontainers:junit-jupiter:1.18.1") // https://mvnrepository.com/artifact/org.testcontainers/testcontainers
    testImplementation("com.github.javafaker:javafaker:1.0.2") // https://github.com/DiUS/java-faker
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

val dbHost = System.getenv("DB_HOST") ?: "localhost"
val dbPort = System.getenv("DB_PORT") ?: 5400
val dbDatabase = System.getenv("DB_NAME") ?: "demo_webflux"
val dbUser = System.getenv("DB_USER") ?: "postgres"
val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"

flyway {
    url = "jdbc:postgresql://${dbHost}:${dbPort}/${dbDatabase}"
    schemas = arrayOf(dbDatabase)
    user = dbUser
    password = dbPassword
}

jooq {
    version.set("3.18.4") // https://mvnrepository.com/artifact/org.jooq/jooq
    configurations {
        create("main") {  // name of the jOOQ configuration
            generateSchemaSourceOnCompilation.set(true)  // default (can be omitted)

            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://${dbHost}:${dbPort}/${dbDatabase}"
                    user = dbUser
                    password = dbPassword
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = dbDatabase
                        excludes = "flyway_schema_history"
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
