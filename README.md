# GroovyXposedBridge
This is a small groovy-based DSL for creating Xposed modules. 
## Why Groovy?
Groovy is a dynamically typed language, that has some nice features in terms of Java reflection. 

It's simple to make DSLs and it's very very easy to access things like a private field on a class.

Most Xposed modules do a lot of reflection, which makes Groovy and Xposed a match made in heaven.

## Getting started

You will need:

0. A gradle-enabled Android project with the [groovy-android pluging](https://github.com/groovy/groovy-android-gradle-plugin)
1. A working Xposed module. Follow the [Xposed tutorial](https://github.com/rovo89/XposedBridge/wiki/Development-tutorial) for more details.
2. GroovyXposedBridge as a dependency, jar og just a plain source file.
3. Something to hack!

## Basic example

For the sake of simplicity, I have taken the Xposed tutorial examples and rewritten them in Groovy.

### Java version
Consult the official Xposed tutorial for a rundown.
```java
import android.graphics.Color;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Tutorial implements IXposedHookLoadPackage {
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.systemui"))
            return;

        findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader, "updateClock",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        TextView tv = (TextView) param.thisObject;
                        String text = tv.getText().toString();
                        tv.setText(text + " :)");
                        tv.setTextColor(Color.RED);
                    }
                });
    }
}
``` 

### GroovyXposedBridge version
The example below shows how the dynamic nature of Groovy helps in a situation like this.

Instead of casting the `param.thisObject` as a `TextView`, you can just call `getText()` directly.

```groovy
import android.graphics.Color
import dk.mehmedbasic.groovyxposedbridge.GroovyXposed

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
                thisObject.setTextColor(Color.RED);
            }
        }
    }
}
``` 
## Methods with parameters
The DSL exposes parameters to the methodhooks.

The java example is from the XDA tutorial written by [hamzahrmalik](http://forum.xda-developers.com/showthread.php?t=2709324):

Instead of casting parameters like this:
``` java
findAndHookMethod("com.android.settings.Settings", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
    @Override
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Bundle bundle = (Bundle) param.args[0];
        if(bundle != null) {
            Log.i("SettingsExample", bundle.getString("test"));
        }
    }
});
```

We can simply write:
```groovy
hookMethod('com.android.settings.Settings') {
    method "onCreate"
    params Bundle.class
    before { Bundle bundle ->
        Log.i("GroovyParameters", bundle?.getString("test"))
    }
}
```
The DSL passes the parameters directly to the hooks, so it's very easy to work with.

# More functions

## Replacing a method
Continuing with the clock example:
```groovy
replaceMethod("com.android.systemui.statusbar.policy.Clock") {
    method "updateClock"
    replace {
        thisObject.setText("No clock!")
    }
}
```

## Hooking a constructor

```groovy
hookConstructor("com.android.systemui.statusbar.policy.Clock") {
    params Context.class
    before { Context ctx ->
        // Do something
    }
}
```

## Looking up a class
There is a `findClass(String name)` method, that uses the classloader from Xposed loadPackageParams.
```groovy
Class clazz = findClass("com.android.systemui.statusbar.policy.Clock")
// Do something
```



# Suggestions and development
This is so far a very small piece of code that I made to help me with some reflection on android.

Feel free to send me suggestions for improvements, since I am certain I haven't covered all the use cases.
