plugins {
    id("dev.kikugie.loom-back-compat")
}

val modVersion = providers.gradleProperty("version").orNull ?: sc.properties.get<String>("mod.version")
val mc = sc.current.version
val modern = sc.current.parsed >= "26.1"
version = modVersion
group = property("mod.group") as String
base.archivesName = "${property("mod.id")}-fabric-$mc"

val requiredJava = when {
    modern -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
    minecraft("com.mojang:minecraft:$mc")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    runConfigs.all {
        runDirectory = rootProject.file("runs/${sc.current.project}")
    }
    providers.gradleProperty("quickPlay").orNull?.let { host -> runs.named("client") { programArgs("--quickPlayMultiplayer", host, "--username", "Dev") } }
}

sourceSets.main {
    java.exclude("com/workshopcraft/smolblocks/neoforge/**", "com/workshopcraft/smolblocks/forge/**")
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
}

tasks {
    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to modVersion,
            "author" to sc.properties.get<String>("mod.author"),
            "description" to sc.properties.get<String>("mod.description"),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "loader" to Regex("\\d+\\.\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value,
            "java" to requiredJava.majorVersion,
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml", "smolblocks.mixins.json")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val name = project.property("mod.name")
        inputs.property("mod_name", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }
}
