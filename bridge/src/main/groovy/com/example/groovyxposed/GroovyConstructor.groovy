package com.example.groovyxposed

import android.content.Context
import dk.mehmedbasic.groovyxposedbridge.GroovyXposed

/**
 * TODO - someone remind me to document this class 
 *
 * @author Jesenko Mehmedbasic
 *         created 5/7/15.
 */
class GroovyConstructor extends GroovyXposed {
    GroovyConstructor() {
        super("com.android.systemui")
    }

    @Override
    void handleLoadedPackage() {
hookConstructor("com.android.systemui.statusbar.policy.Clock") {
    params Context.class
    before { Context ctx ->
        // Do something
    }
}
    }
}
