// https://dev.to/kuperadrian/how-to-setup-a-unit-testable-jenkins-shared-pipeline-library-2e62
// this interface will be mocked for our unit tests. But for regular execution of our library we still need an default implementation

package org.nut.dynamatrix.ioc

import org.nut.dynamatrix.IStepExecutor
import org.nut.dynamatrix.StepExecutor

class DefaultContext implements IContext, Serializable {
    // the same as in the StepExecutor class
    private _steps

    DefaultContext(steps) {
        this._steps = steps
    }

    @Override
    IStepExecutor getStepExecutor() {
        return new StepExecutor(this._steps)
    }
}

