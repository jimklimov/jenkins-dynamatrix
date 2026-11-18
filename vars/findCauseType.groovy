#!/usr/bin/env groovy
/// ^^^ For syntax highlighters

import org.nut.dynamatrix.Utils;

/** findCauseType() - Recursively search build causes for a matching type.
 * Adapted from the reference project's implementation.
 */
def call(c, t) {
    try {
        for (uc in c.getCauses()) {
            if (uc in t) return uc
            def r = findCauseType(uc, t)
            if (r != null) return r
        }
        return null
    } catch(Throwable tt) {}

    try {
        for (uc in c.getUpstreamCauses()) {
            if (uc in t) return uc
            def r = findCauseType(uc, t)
            if (r != null) return r
        }
        return null
    } catch(Throwable tt) {}

    return null
}
