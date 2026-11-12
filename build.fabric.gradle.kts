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

val legacyDir = if (sc.current.parsed < "1.21") "1.20.1" else "1.21.1"

sourceSets.main {
    java.exclude("com/workshopcraft/smolblocks/neoforge/**", "com/workshopcraft/smolblocks/forge/**", "com/workshopcraft/smolblocks/legacy/forgelike/**",
        "com/workshopcraft/smolblocks/legacy/neoforge/**", "com/workshopcraft/smolblocks/legacy/forge/**")
    if (modern) java.exclude("com/workshopcraft/smolblocks/legacy/**")
    else java.exclude("com/workshopcraft/smolblocks/client/**", "com/workshopcraft/smolblocks/fabric/client/**")
    if (!modern) resources.setSrcDirs(listOf(rootProject.file("src/legacy/$legacyDir"), rootProject.file("src/main/resources")))
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
            "client_entry" to if (modern) "com.workshopcraft.smolblocks.fabric.client.SmolBlocksFabricClient" else "com.workshopcraft.smolblocks.legacy.fabric.LegacyFabricClient",
            "mixin_package" to if (modern) "client.mixin" else "legacy.client.mixin",
            "mixin_class" to if (modern) "FluidRendererMixin" else "LiquidBlockRendererMixin",
            "mixin_java" to if (requiredJava == JavaVersion.VERSION_17) "17" else "21",
            "refmap" to "",
        )
        inputs.properties(props)
        filesMatching(listOf("fabric.mod.json", "smolblocks.mixins.json", "smolblocks.fabric.mixins.json")) { expand(props) }
        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml")
        if (!modern) exclude("assets/smolblocks/items/**")
        // Fabric builds the micro block models in code, and this file is in the NeoForge format
        if (modern) exclude("assets/smolblocks/blockstates/micro.json")
        if (sc.current.parsed < "1.21") exclude("data/smolblocks/recipe/**", "data/smolblocks/tags/block/**")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val name = project.property("mod.name")
        inputs.property("mod_name", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }
}
