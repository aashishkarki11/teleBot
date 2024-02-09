plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Use JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Include TelegramBots library
    implementation("org.telegram:telegrambots:5.3.0")

    // Include org.json library
    implementation("org.json:json:20231013") // or the latest version available

    compileOnly("org.projectlombok:lombok:1.18.22")
    implementation("org.projectlombok:lombok:1.18.22")
}

tasks.test {
    useJUnitPlatform()
}
