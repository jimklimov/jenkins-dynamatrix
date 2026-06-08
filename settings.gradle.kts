import org.gradle.kotlin.dsl.mavenCentral
import org.gradle.kotlin.dsl.repositories

pluginManagement {
    repositories {
        gradlePluginPortal()

        //noinspection JCenterRepository
        // https://blog.gradle.org/jcenter-shutdown says it will remain R/O
        //jcenter()
        mavenCentral()

        maven (url = "https://plugins.gradle.org/m2/")

        //maven (url = "https://repo.jenkins-ci.org/releases/")
        maven (url = "https://repo.jenkins-ci.org/incrementals/")
        // Mirror not served anymore // maven (url = "https://repo.jenkins-ci.org/public/")

        // Note: this one reports "403 Forbidden" if an URL is bad -
        // check spelling of the artifact (too few/many components etc.)
        maven (url = "https://mvnrepository.com/artifact/")
    }
}

plugins {
/*
    // ex "build-scan"
    id "com.gradle.enterprise" version "3.7.2"
*/
/*
    // or another
    id("com.gradle.develocity") version "4.4.1"
 */
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven {
            name = "jenkins"
            url = uri("https://repo.jenkins-ci.org/public/")
        }
    }
    versionCatalogs {
        create("jenkinsPlugins") {
            from(files("gradle/jenkins.versions.toml"))
        }
    }
}

/*
// See plugin above (commented away too)
develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        publishing.onlyIf { System.getenv("DEVELOCITY_PUBLISH") == "1" }
    }
}
*/

rootProject.name = "jenkins-dynamatrix"
