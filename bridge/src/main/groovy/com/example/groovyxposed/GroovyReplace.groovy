package com.example.groovyxposed

import dk.mehmedbasic.groovyxposedbridge.GroovyXposed

/**
 * A replace method in GroovyXposed
 *
 * @author Jesenko Mehmedbasic
 */
class GroovyReplace extends GroovyXposed {
    GroovyReplace() {
        super("com.android.systemui")
    }

    @Override
    void handleLoadedPackage() {
        replaceMethod("com.android.systemui.statusbar.policy.Clock") {
            method "updateClock"
            replace {
                thisObject.setText("No clock!")
            }
        }
    }
}
