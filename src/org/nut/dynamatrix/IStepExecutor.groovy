// https://dev.to/kuperadrian/how-to-setup-a-unit-testable-jenkins-shared-pipeline-library-2e62
// The Interface For Step Access  -- that will be used by all classes to access the regular Jenkins steps like sh or error

package org.nut.dynamatrix;

interface IStepExecutor {
    int sh(String command)
    void error(String message)
    void node(String name, Closure closure)
    // add more methods for respective steps if needed
}

