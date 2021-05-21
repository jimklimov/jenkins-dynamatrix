import org.nut.dynamatrix.ioc.ContextRegistry

void call(String stashName, Closure scmbody = null) {
    // Optional closure can fully detail how the code is checked out
    ContextRegistry.registerDefaultContext(this)

    /* clean up our workspace */
    deleteDir()
    /* clean up tmp directory */
    dir("${workspace}@tmp") {
        deleteDir()
    }
    /* clean up script directory */
    dir("${workspace}@script") {
        deleteDir()
    }
    if (scmbody == null) {
        checkout scm
    } else {
        scmbody()
    }
    stash stashName
} // stashCleanSrc()
