import org.nut.dynamatrix.*;
import org.nut.dynamatrix.ioc.ContextRegistry

/*
 * Run one combination of settings in the matrix for default compiler
 * using its default version of the C/C++ standards
 */
void call(String BUILD_TYPE, String PLATFORM) {
    // NOTE: Analysis below just assumes the default compiler would be gcc
    // TODO: Detect the reality, or pass the arg?..
    ContextRegistry.registerDefaultContext(this)

    warnError(message: 'Build-and-check step failed, proceeding to cover whole matrix') {
        sh """ BUILD_TYPE="${BUILD_TYPE}" ./ci_build.sh """
    }
    script {
        def id = "Distcheck:${BUILD_TYPE}@${PLATFORM}"
        def i = scanForIssues tool: gcc(name: id)
        //def i = scanForIssues tool: clang(name: id)
        //dynamatrixGlobalState.issueAnalysis << i
        publishIssues issues: [i], filters: [includePackage('io.jenkins.plugins.analysis.*')]
    }
} // buildMatrixCellDistcheck()
