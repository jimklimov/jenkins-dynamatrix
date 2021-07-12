import org.nut.dynamatrix.dynamatrixGlobalState
import org.nut.dynamatrix.*

/*
 * Run one combination of settings in the matrix for chosen compiler, etc.
 */
void call(dynacfgPipeline = [:], DynamatrixSingleBuildConfig dsbc = null, String stageName = null) {
    // Values in env.* inspected below come from the anticipated build
    // matrix settings (agent labels, etc.), configureEnvvars.groovy and
    // tool configuration like autotools.groovy or ci_build.groovy

    // NOTE: Currently the values set ONLY by configureEnvvars.groovy
    // are not yet set and so not exposed in env[] by the time we build
    // the ID string below. In fact, it may be too complex to reliably
    // extract CONFIG_ENVVARS set in shell back to groovy and shell again.

    // TODO: Pass into build additional hints that may be in dsbc, e.g.
    // clioptSet - but gotta decide to which tool they should go and when
    // (e.g. configure script takes some opts, ci_build normally does not)

    // for analysis part after the build
    def compilerTool = null
    // Allowed elements are characters, digits, dashes and underscores
    // (more precisely, the ID must match the regular expression `\p{Alnum}[\p{Alnum}-_]*`
    def id = ""

        def msg = "Building with "
        if (env?.COMPILER) {
            id = env.COMPILER.toUpperCase().trim()
            switch (env.COMPILER) {
                // TODO: Handle alternately-named cross-compilers like packaged
                // like "arm-linux-gnueabi-gcc(-10)" or third-party snapshots
                // named like "arm-linux-gnueabi-20140823-20131011-gcc-4.8.1"
                // Note that would also need some cooperation from agent labels
                // and generic "${COMPILER}VER" support, possibly some use of
                // dynamatrixAxesCommonOpts and/or dynamatrixAxesCommonEnv
                case ['gcc', 'GCC']:
                    compilerTool = 'gcc'
                    if (env?.GCCVER) {
                        id += "-${env.GCCVER}"
                        try {
                            if (env?.GCCVER.replaceAll(/\..*$/, '').toInteger() < 4) {
                                compilerTool = 'gcc3'
                            }
                        } catch (Throwable n) {}
                    }
                    break
                case ['clang', 'CLANG']:
                    compilerTool = 'clang'
                    if (env?.CLANGVER) id += "-${env.CLANGVER}"
                    break
                default:
                    try {
                        if (env["${env.COMPILER}VER"]) {
                            id += "-" + env["${env.COMPILER}VER"]
                        }
                    } catch (Throwable t) {} // ignore
                    break
            }
        } else {
            if (env?.GCCVER) {
                id = "GCC-${env.GCCVER}"
                compilerTool = 'gcc'
                try {
                    if (env?.GCCVER.replaceAll(/\..*$/, '').toInteger() < 4) {
                        compilerTool = 'gcc3'
                    }
                } catch (Throwable n) {}
            } else if (env?.CLANGVER) {
                id = "CLANG-${env.CLANGVER}"
                compilerTool = 'clang'
            } else {
                echo "WARNING: env got no COMPILER, and no supported {COMPILER}VER... Proceeding by default..."
            }
        }
        if (id != "") msg += "${id} "

        if (env?.CSTDVERSION_c) {
            def tmp = DynamatrixSingleBuildConfig.C_stdarg(env?.CSTDVARIANT, env?.CSTDVERSION_c, false)
            id += "_${tmp}"
            msg += "STD=${tmp} "
        } else {
            if (env?.STDARG) {
                id += "_${env.STDARG}"
                msg += ("STD=${env.STDARG} " - ~/-std=/)
            }
        }

        if (env?.CSTDVERSION_cxx) {
            def tmp = DynamatrixSingleBuildConfig.C_stdarg(env?.CSTDVARIANT, env?.CSTDVERSION_cxx, true, true)
            id += "_${tmp}".replaceAll(/\+/, 'x')
            msg += "STD=${tmp} "
        } else {
            if (env?.STDXXARG) {
                id += "_${env.STDXXARG}"
                msg += ("STD=${env.STDXXARG} " - ~/-std=/)
            }
        }

        if (env?.BUILD_WARNOPT) {
            id += "_WARN=${env.BUILD_WARNOPT}"
            msg += "WARN=${env.BUILD_WARNOPT} "
        }

        if (env?.ARCHARG || env?.BITSARG || env?.ARCH_BITS || env?.BITS || env["ARCH${ARCH_BITS}"] || env["ARCH${BITS}"] || env?.OS_FAMILY || env?.OS_DISTRO) {
            msg += "on "
            if (env["ARCH${ARCH_BITS}"]) {
                id += "_${env["ARCH${ARCH_BITS}"]}"
                msg += "${env["ARCH${ARCH_BITS}"]} "
            } else if (env["ARCH${BITS}"]) {
                id += "_${env["ARCH${BITS}"]}"
                msg += "${env["ARCH${BITS}"]} "
            } else {
                if (env?.ARCHARG) {
                    id += "_${env.ARCHARG}"
                    msg += ("${env.ARCHARG} " - ~/-arch[ =]/)
                }
            }

            if (env?.ARCH_BITS) {
                id += "_${env.ARCH_BITS}"
                msg += "${env.ARCH_BITS} "
            } else if (env?.BITS) {
                id += "_${env.BITS}"
                msg += "${env.BITS} "
            } else {
                if (env?.BITSARG) {
                    id += "_${env.BITSARG}"
                    msg += ("${env.BITSARG} " - ~/-m/) + "-bit "
                }
            }

            if (env?.OS_FAMILY || env?.OS_DISTRO) {
                if (env?.OS_FAMILY && env?.OS_DISTRO) {
                    id += "_${env.OS_FAMILY}-${env.OS_DISTRO}"
                    msg += "${env.OS_FAMILY}-${env.OS_DISTRO} "
                } else {
                    // Only one of those strings is present
                    id += "_${env.OS_FAMILY}${env.OS_DISTRO}"
                    msg += "${env.OS_FAMILY}${env.OS_DISTRO} "
                }
            }

            msg += "platform"
        }

        id = id.trim().replaceAll(/\\s+/, '_')
        if (stageName)
            msg = msg.trim() + " for ${stageName}"


        // Strive for unique name prefix across many similar builds executed
        def archPrefix = id
        if (stageName)
            archPrefix += "--" + stageName
        archPrefix = archPrefix.trim().replaceAll(/\s+/, '').replaceAll(/[^\p{Alnum}-_]+/, '-')
        if (archPrefix.length() > 250) { // Help filesystems that limit filename size
            archPrefix = "MD5_" + archPrefix.md5()
        }

        // Build a multiline shell script
        // Split that into many shell steps (each with configureEnvvars
        // re-run or some re-import of the first generated value, if needed),
        // and/or sequential stages to visualize in BO UI build progress
        def cmdCommonLabel = ""
        def cmdPrepLabel = ""
        def cmdBuildLabel = ""
        def cmdTest1Label = ""
        def cmdTest2Label = ""

        def cmdCommon = """ """
        def cmdPrep = ""
        def cmdBuild = ""
        def cmdTest1 = ""
        def cmdTest2 = ""
        if (!dynacfgPipeline?.traceBuildShell) {
            cmdCommon = """ set +x
"""
        }

        if (dynacfgPipeline?.buildSystem) {
            cmdCommonLabel = "With ${dynacfgPipeline.buildSystem}: "
        }

        if (dynacfgPipeline?.configureEnvvars) {
            // Might be better to evict into cmdPrep alone, but for e.g.
            // the ci_build.sh tooling defaults, we only call "check"
            // and that handles everything for the BIUILD_TYPE requested
            cmdCommon += """ ${dynacfgPipeline?.configureEnvvars}
"""
            cmdCommonLabel += "configureEnvvars "
        }

        if (dynacfgPipeline?.prepconf) {
            cmdPrep += """ ${dynacfgPipeline?.prepconf}
"""
            cmdPrepLabel += "prepconf "
        }

        if (dynacfgPipeline?.configure) {
            cmdPrep += """ ${dynacfgPipeline?.configure}
"""
            cmdPrepLabel += "configure "
        }

        if (dynacfgPipeline?.buildQuiet) {
            cmdBuild += """ ${dynacfgPipeline?.buildQuiet}
"""
            cmdBuildLabel += "buildQuiet "
        } else if (dynacfgPipeline?.build) {
            cmdBuild += """ ${dynacfgPipeline?.build}
"""
            cmdBuildLabel += "build "
        }

        if (dynacfgPipeline?.check) {
            cmdTest1 += """ ${dynacfgPipeline?.check}
"""
            cmdTest1Label += "check "
        }

        if (dynacfgPipeline?.distcheck) {
            cmdTest2 += """ ${dynacfgPipeline?.distcheck}
"""
            cmdTest2Label += "distcheck "
        }

        if (stageName) {
            if (cmdPrepLabel != "")
                cmdPrepLabel = cmdPrepLabel.trim() + " for ${stageName}"
            if (cmdBuildLabel != "")
                cmdBuildLabel = cmdBuildLabel.trim() + " for ${stageName}"
            if (cmdTest1Label != "")
                cmdTest1Label = cmdTest2Label.trim() + " for ${stageName}"
            if (cmdTest2Label != "")
                cmdTest2Label = cmdTest2Label.trim() + " for ${stageName}"
        }

        def shRes = 0
        stage('Prep') {
            echo msg
            //if (dynamatrixGlobalState.enableDebugTrace)
            //if (dynacfgPipeline?.configureEnvvars)
                sh label: 'Report compilers', script: cmdCommon + """ ( eval \$CONFIG_ENVVARS; echo "CC: \$CC => `command -v "\$CC"`"; echo "CXX: \$CXX => `command -v "\$CXX"`" ; hostname; ) ; """
            if (cmdPrep != "") {
                def res = sh (script: cmdCommon + cmdPrep, returnStatus: true, label: (cmdCommonLabel + cmdPrepLabel.trim()))
                if (res != 0) {
                    shRes = res
                    unstable "FAILED 'Prep'" + (stageName ? " for ${stageName}" : "")
                }
            }
        }

        if (cmdBuild != "" && shRes == 0) {
            stage('Build') {
                def res = sh (script: cmdCommon + cmdBuild, returnStatus: true, label: (cmdCommonLabel + cmdBuildLabel.trim()))
                if (res != 0) {
                    shRes = res
                    unstable "FAILED 'Build'" + (stageName ? " for ${stageName}" : "")
                }
            }
        }

        def nameTest1 = "Test1"
        def nameTest2 = "Test2"

        if ( (cmdTest1 != "" && cmdTest2 == "")
        &&   (cmdTest1 == "" && cmdTest2 != "")
        ) {
            nameTest1 = "Test"
            nameTest2 = "Test"
        }

        if (cmdTest1 != "" && shRes == 0) {
            stage(nameTest1) {
                def res = sh (script: cmdCommon + cmdTest1, returnStatus: true, label: (cmdCommonLabel + cmdTest1Label.trim()))
                if (res != 0) {
                    shRes = res
                    unstable "FAILED 'Test1'" + (stageName ? " for ${stageName}" : "")
                }
            }
        }

        if (cmdTest2 != "" && shRes == 0) {
            stage(nameTest2) {
                def res = sh (script: cmdCommon + cmdTest2, returnStatus: true, label: (cmdCommonLabel + cmdTest2Label.trim()))
                if (res != 0) {
                    shRes = res
                    unstable "FAILED 'Test2'" + (stageName ? " for ${stageName}" : "")
                }
            }
        }

    stage('Collect results') {
        // Capture this after all the stages, different tools
        // might generate the files at different times
        sh """ if [ -s config.log ]; then gzip < config.log > '${archPrefix}--config.log.gz' || true ; fi """
        archiveArtifacts (artifacts: "${archPrefix}--*", allowEmptyArchive: true)

        def i = null
        switch (compilerTool) {
            case 'gcc':
                i = scanForIssues tool: gcc(id: id)
                break
            case 'gcc3':
                i = scanForIssues tool: gcc3(id: id)
                break
            case 'clang':
                i = scanForIssues tool: clang(id: id)
                break
        }
        if (i != null) {
            if (dynacfgPipeline?.delayedIssueAnalysis) {
                // job should call doSummarizeIssues() in the end
                dynamatrixGlobalState.issueAnalysis << i
            } else {
                publishIssues issues: [i], filters: [includePackage('io.jenkins.plugins.analysis.*')]
            }
        }
    }

    if (shRes != 0) {
        def msgFail = 'Build-and-check step failed, proceeding to cover the rest of matrix'
        if (dsbc?.isAllowedFailure) {
            unstable msgFail
        } else {
            error msgFail
        }
    }
} // buildMatrixCellCI()