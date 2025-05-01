# grails-profiles
Consolidated Home for Grails Profiles

## How to configure local development environment for Grails Profiles testing

### Build the latest grails-shell CLI snapshot

    ./gradlew assemble

### Configure grails-shell CLI to use mavenLocal() profiles

Create **USER_HOME/.grails/settings.groovy**

    grails {
        profiles {
            repositories {
            mavenLocal()
            mavenCentral()
            apacheSnapshot {
                url = "https://repository.apache.org/content/groups/snapshots"
                snapshotsEnabled = true
                }
            }
        }
    }


### Publish profiles to mavenLocal()
    ./gradlew build
    ./gradlew publishToMavenLocal

### Use grails-shell CLI -SNAPSHOT to generate test application from profile
    mkdir testProfiles
    cd testProfiles/
    {path to grails-shell cli snapshot}/grails create-app TestProfile
    ./gradlew dependencies --refresh-dependencies

### Iterative testing of profile changes
- Review TestProfile application
- force the application to pull the mavenLocal() profile into the generated application with **./gradlew dependencies --refresh-dependencies**
- makes changes to grails-profiles project
- republish grails-profiles to mavenLocal()
- delete contents of TestProfile application
- regenerate with **./grails create-app TestProfile**
- Repeat





