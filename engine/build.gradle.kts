plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin { jvmToolchain(17) }

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

// Regenera los niveles de campaña (assets del módulo :app) con los propios generadores del motor.
tasks.register<JavaExec>("generateCampaign") {
    group = "puzzles"
    description = "Genera app/src/main/assets/campaign/*.json con solución única garantizada"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.tuapp.puzzles.tools.GenerateCampaignKt")
    args(rootProject.layout.projectDirectory.dir("app/src/main/assets/campaign").asFile.absolutePath)
}
