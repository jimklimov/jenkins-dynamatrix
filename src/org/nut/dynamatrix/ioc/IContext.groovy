// https://dev.to/kuperadrian/how-to-setup-a-unit-testable-jenkins-shared-pipeline-library-2e62
// Basic Dependency Injection -- Because we don't want to use the above implementation in our unit tests, we will setup some basic dependency injection in order to swap the above implementation with a mock during unit tests.

package org.nut.dynamatrix.ioc

import org.nut.dynamatrix.IStepExecutor

interface IContext {
    IStepExecutor getStepExecutor()
}

