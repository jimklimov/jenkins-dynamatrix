import org.nut.dynamatrix.ioc.ContextRegistry

void call(String stashName) {
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
    unstash stashName
} // unstashCleanSrc()
