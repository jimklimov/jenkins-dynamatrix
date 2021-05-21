// https://dev.to/kuperadrian/how-to-setup-a-unit-testable-jenkins-shared-pipeline-library-2e62
// The interface IStepExecutor is neat, because it can be mocked inside our unit tests. That way our classes become independent to Jenkins itself. For now, let's add an implementation that will be used in our vars Groovy scripts:

package org.nut.dynamatrix;

class StepExecutor implements IStepExecutor {
    // this will be provided by the vars script and 
    // let's us access Jenkins steps
    private _steps 

    StepExecutor(steps) {
        this._steps = steps
    }

    @Override
    int sh(String command) {
        this._steps.sh returnStatus: true, script: "${command}"
    }

    @Override
    void error(String message) {
        this._steps.error(message)
    }

    @Override
    void node(String name, Closure closure) {
        this._steps.node(name, closure)
    }
}

