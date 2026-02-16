plugins {
    id("java-library")
    alias(libs.plugins.lombok)
    alias(libs.plugins.checkerframework)
}

repositories {
    maven("https://repo.opencollab.dev/maven-snapshots/")
    mavenCentral()
}

dependencies {
    api(project(":Protocol:common"))
    api(platform(libs.fastutil.bom))
    api(libs.netty.buffer)
    api(libs.fastutil.long.common)
    api(libs.fastutil.long.obj.maps)
    api(libs.jose4j)
    api(libs.nbt)
    api(libs.adventure.text.serializer.legacy)
    api(libs.adventure.text.serializer.json)
    implementation(libs.jackson.annotations)
    
    // CloudburstMC Protocol dependencies (versão Beta12)
    api("org.cloudburstmc.protocol:bedrock-codec:3.0.0.Beta12-SNAPSHOT")
    api("org.cloudburstmc.protocol:bedrock-connection:3.0.0.Beta12-SNAPSHOT")
    api("org.cloudburstmc.protocol:common:3.0.0.Beta12-SNAPSHOT")
    
    // Tests
    testImplementation(libs.junit)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "org.cloudburstmc.protocol.bedrock.codec")
    }
}
