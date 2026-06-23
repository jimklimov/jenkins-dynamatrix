package org.nut.dynamatrix;

// Inspired by https://github.com/mkobit/jenkins-pipeline-shared-library-example/blob/master/test/integration/groovy/com/mkobit/libraryexample/VarsExampleJunitTest.groovy
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
//import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

class genericPipelineTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule()

/*
    @Before
    void configureGlobalGitLibraries() {
        RuleBootstrapper.setup(rule)
    }
*/

    @Test
    void "testing env interaction, inline method definitions and standard pipeline script step calling"() {
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
    void "Testing list of loaded plugins"() {
        final CpsFlowDefinition flow = new CpsFlowDefinition('''
			import jenkins.model.Jenkins
            def plugins = jenkins.model.Jenkins.instance.getPluginManager().getPlugins()
            echo "=== The following ${plugins?.size()} plugins are installed:"
            String l = ""
			plugins.each {
				l += "${it.getDisplayName()} (${it.getShortName()}): ${it.getVersion()}\\n"
			}
			echo "${l}"
            echo "=== End of list"
        ''', false)

        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'p')
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
    }

    /** Make sure gradle setup lets us run all the integration tests
     * (some fail without prerequisites below, solutions to which
     * are on the recipe's side rather than JSL or test code base).
     */
    @Test
    void "Testing durable task plugin availability - sh step"() {
        final CpsFlowDefinition flow = new CpsFlowDefinition('''
            node {
                sh 'echo Hello from Shell'
            }
        ''', true)

        final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'p')
        workflowJob.definition = flow
        WorkflowRun run = workflowJob.scheduleBuild2(0).get();

        rule.assertBuildStatus(hudson.model.Result.SUCCESS, run)
        rule.assertLogContains("Hello from Shell", run)
    }
}
