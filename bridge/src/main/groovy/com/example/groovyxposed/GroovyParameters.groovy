package com.example.groovyxposed

import android.os.Bundle
import android.util.Log
import dk.mehmedbasic.groovyxposedbridge.GroovyXposed

/**
 * Groovy class with params
 *
 * @author Jesenko Mehmedbasic
 */
class GroovyParameters extends GroovyXposed {
    GroovyParameters() {
        super("com.android.settings")
    }

    @Override
    void handleLoadedPackage() {
        hookMethod('com.android.settings.Settings') {
            method "onCreate"
            params Bundle.class
            before { Bundle bundle ->
                Log.i("GroovyParameters", "Bundle was: " + bundle)
                Log.i("GroovyParameters", "Test string: " + bundle?.getString("test"))
            }
        }
    }
}
