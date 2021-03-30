plugins {
	java
	kotlin("jvm") version "1.4.30"
	id("org.jetbrains.intellij") version "0.7.2"
	id("org.jetbrains.grammarkit") version "2020.3.2"
}

group = "com.windea"
version = "0.1"

intellij {
	version = "2020.3.2"
	pluginName = "Paradox Language Support"
}

buildscript {
	repositories {
		maven("https://maven.aliyun.com/nexus/content/groups/public")
		mavenCentral()
		jcenter()
	}
}

repositories {
	maven("https://dl.bintray.com/kotlin/kotlin-eap")
	maven("https://maven.aliyun.com/nexus/content/groups/public")
	mavenCentral()
	jcenter()
}

dependencies{
	implementation(kotlin("stdlib"))
	implementation(kotlin("reflect"))
}

sourceSets {
	main {
		java.srcDir("src/main/gen")
		java.srcDir("src/main/kotlin")
	}
}

tasks {
	compileKotlin {
		kotlinOptions.jvmTarget = "8"
	}
	compileTestKotlin {
		kotlinOptions.jvmTarget = "8"
	}
}

grammarKit {
	jflexRelease = "1.7.0-2"
	grammarKitRelease = "2020.3.2"
}
