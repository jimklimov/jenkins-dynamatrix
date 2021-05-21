// https://dev.to/kuperadrian/how-to-setup-a-unit-testable-jenkins-shared-pipeline-library-2e62
// To finish up our basic dependency injection setup, let's add a "context registry" that is used to store the current context (DefaultContext during normal execution and a Mockito mock of IContext during unit tests):

package org.nut.dynamatrix.ioc

class ContextRegistry implements Serializable {
    private static IContext _context

    static void registerContext(IContext context) {
        _context = context
    }

    static void registerDefaultContext(Object steps) {
        _context = new DefaultContext(steps)
    }

    static IContext getContext() {
        return _context
    }
}

