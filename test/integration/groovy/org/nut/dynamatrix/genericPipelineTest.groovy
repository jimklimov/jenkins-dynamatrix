package org.nut.dynamatrix;

// Inspired by https://github.com/mkobit/jenkins-pipeline-shared-library-example/blob/master/test/integration/groovy/com/mkobit/libraryexample/VarsExampleJunitTest.groovy
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.WithTimeout;

/**
 * NOTE: Sometimes JenkinsRule takes a long while to start,
 * timing out (default 180s) before the actual test-case
 * pipeline begins to run. A second test case usually
 * succeeds as Jenkins is just about ready by then.
 * We bump this for all cases to succeed better.
 */
@WithJenkins
class genericPipelineTest {
    @Test
    @WithTimeout(1500)
    void testCStdPairSortingInPipeline(JenkinsRule rule) {
        final CpsFlowDefinition flow = new CpsFlowDefinition('''
            import org.nut.dynamatrix.UtilsCStd

            def pairs = [
                [c: '17', cxx: '17'],
                [c: '99', cxx: '11'],
                [c: '99', cxx: '98'],
                [c: '11', cxx: '11']
            ]
            assert UtilsCStd.resolveExtremeCStdPairs('GCCVER=10', 'MIN', pairs, []) == [[c: '99', cxx: '98']]
            assert UtilsCStd.resolveExtremeCStdPairs('GCCVER=10', 'MAX', pairs, []) == [[c: '17', cxx: '17']]
            assert UtilsCStd.resolveExtremeCStdPairs('GCCVER=10', 'NOT_MIN', pairs, []) == [
                [c: '99', cxx: '11'], [c: '11', cxx: '11'], [c: '17', cxx: '17']
            ]
            assert UtilsCStd.resolveExtremeCStdPairs('GCCVER=10', 'MAX',
                [[c: '99', cxx: '98'], [c: 'MAX', cxx: 'MAX']], []) == [[c: '2y', cxx: '2c']]
            echo 'C standard pair sorting passed'
        '''.stripIndent(), false)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'pCStdPairs')
        workflowJob.definition = flow
        final WorkflowRun run = rule.buildAndAssertSuccess(workflowJob)

        rule.assertLogContains('C standard pair sorting passed', run)
        rule.assertLogNotContains('expected to call', run)
    }

    @Test
    void "Example testing env interaction, inline method definitions and standard pipeline script step calling"(JenkinsRule rule) {
        final CpsFlowDefinition flow = new CpsFlowDefinition('''
        def evenOrOdd (int n) {
            if (n % 2 == 0) {
                echo "The build number is even"
            } else {
                echo "The build number is odd"
            }
        }

        evenOrOdd(env.BUILD_NUMBER as int)
    '''.stripIndent(), true)
        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
        workflowJob.definition = flow

        final WorkflowRun firstResult = rule.buildAndAssertSuccess(workflowJob)
        rule.assertLogContains('The build number is odd', firstResult)

        final WorkflowRun secondResult = rule.buildAndAssertSuccess(workflowJob)
        rule.assertLogContains('The build number is even', secondResult)
    }

    /** Half test, half JSL/test developer troubleshooting aid */
    @Test
    @WithTimeout(1500)
    void "Testing list of loaded plugins"(JenkinsRule rule) {
        final CpsFlowDefinition flow = new CpsFlowDefinition('''
            //import jenkins.model.Jenkins
            def plugins = jenkins.model.Jenkins.instance.getPluginManager().getPlugins()
            echo "=== The following ${plugins?.size()} plugins are installed:"
            String l = ""
			plugins.each {
				l += "${it.getDisplayName()} (${it.getShortName()}): ${it.getVersion()}\\n"
			}
			echo "${l}"
            echo "=== End of list"
        ''', false)

        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'pList')
        workflowJob.definition = flow
        WorkflowRun run = workflowJob.scheduleBuild2(0).get()

        rule.assertBuildStatus(hudson.model.Result.SUCCESS, run)

        def plugins = rule.getInstance().pluginManager.plugins
        System.err.println("=== For reference, JenkinsRule plugin list (${plugins?.size()}) reported by test harness:".toString())
        String l = ""
        plugins?.each {
            l += "${it.getDisplayName()} (${it.getShortName()}): ${it.getVersion()}\n"
        }
        System.err.println(l + "\n=== End of list")

        rule.assertLogContains("durable-task", run)
        rule.assertLogContains("Durable Task Plugin", run)
    }

    /** Make sure gradle setup lets us run all the integration tests
     * (some fail without prerequisites below, solutions to which
     * are on the recipe's side rather than JSL or test code base).
     */
    @Test
    @WithTimeout(1500)
    void "Testing durable task plugin availability - sh step"(JenkinsRule rule) {
        final CpsFlowDefinition flow = new CpsFlowDefinition('''
            node {
                // Typically should show 'built-in' here:
                echo "Hello from Jenkins node: ${env.NODE_NAME}"
                sh 'echo Hello from Shell'
            }
        ''', true)

        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'pSh')
        workflowJob.definition = flow
        WorkflowRun run = workflowJob.scheduleBuild2(0).get();

        rule.assertBuildStatus(hudson.model.Result.SUCCESS, run)
        rule.assertLogContains("Hello from Shell", run)
    }
}
