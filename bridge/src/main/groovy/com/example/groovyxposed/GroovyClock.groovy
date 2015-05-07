package com.example.groovyxposed

import android.graphics.Color
import dk.mehmedbasic.groovyxposedbridge.GroovyXposed

/**
 * Groovy version of the XposedBridge tutorial
 *
 * @author Jesenko Mehmedbasic
 */
class GroovyClock extends GroovyXposed {
    GroovyClock() {
        super("com.android.systemui")
    }

    @Override
    void handleLoadedPackage() {
        hookMethod('com.android.systemui.statusbar.policy.Clock') {
            method "updateClock"
            after {
                def text = thisObject.getText()
                thisObject.setText(text + " :)");
                thisObject.setTextColor(Color.BLUE);
            }
        }
    }
}
