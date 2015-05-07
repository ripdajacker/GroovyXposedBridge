package dk.mehmedbasic.groovyxposedbridge

import android.util.Log
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage

import static de.robv.android.xposed.XposedHelpers.*

/**
 * The Xposed DSL
 *
 * @author Jesenko Mehmedbasic
 */
@SuppressWarnings("UnnecessaryQualifiedReference")
abstract class GroovyXposed implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {
    private static final Object UNDEFINED = new Object()


    protected XC_LoadPackage.LoadPackageParam loadPackageParam
    protected XC_InitPackageResources.InitPackageResourcesParam initPackageResourcesParam
    protected IXposedHookZygoteInit.StartupParam startupParam

    private String targetPackage
    private String modulePath

    def GroovyXposed(String targetPackage) {
        this.targetPackage = targetPackage
    }

    @Override
    final void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam initPackageResourcesParam) throws Throwable {
        if (!targetPackage.equals(initPackageResourcesParam.packageName)) {
            return
        }
        this.initPackageResourcesParam = initPackageResourcesParam
        handleInitPackageResources()
    }


    @Override
    final void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!targetPackage.equals(loadPackageParam.packageName)) {
            return
        }
        this.loadPackageParam = loadPackageParam
        handleLoadedPackage()
    }

    @Override
    void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        modulePath = startupParam.modulePath
    }

    protected Class<?> findClass(String className) {
        return findClass(className, loadPackageParam.classLoader)
    }

    /**
     * Hook a method
     *
     * @param className the class to hook.
     */
    def hookMethod(String className, Closure closure) {
        hookMethod(findClass(className), closure)
    }

    /**
     * Hook a method
     *
     * @param className the class to hook.
     */
    def hookMethod(Class clazz, Closure closure) {
        MethodHook hook = createHook(closure)
        def typesAndCallback = hook.params + [new GroovyMethodHook(hook)]
        findAndHookMethod(clazz, hook.name, typesAndCallback)
    }

    /**
     * Replace a method
     *
     * @param className the class to hook.
     */
    def replaceMethod(String className, Closure closure) {
        replaceMethod(findClass(className), closure)
    }

    /**
     * Replace a method
     *
     * @param clazz the class to hook.
     */
    def replaceMethod(Class clazz, Closure closure) {
        MethodHook hook = createHook(closure)
        def typesAndCallback = hook.params + [new GroovyMethodReplacement(hook)]
        findAndHookMethod(clazz, hook.name, typesAndCallback)
    }

    def hookConstructor(String className, Closure closure) {
        hookConstructor(findClass(className), closure)
    }

    def hookConstructor(Class clazz, Closure closure) {
        MethodHook hook = createHook(closure)
        def typesAndCallback = hook.params + [new GroovyMethodHook(hook)]
        findAndHookConstructor(clazz, hook.name, typesAndCallback)
    }

    /**
     * Creates a hook
     *
     * @param closure the closure to hydrate
     *
     * @return the created hook.
     */
    private MethodHook createHook(Closure closure) {
        def hook = new MethodHook()
        def code = closure.rehydrate(hook, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        hook
    }

    /**
     * Holding the DSL hook
     */
    private static final class MethodHook {
        private String name;
        private Object[] params = [];

        private Closure before = {}
        private Closure after = {}
        private Closure replace = {}

        private int priority = 50;

        void method(String name) {
            this.name = name
        }

        void params(Object... objects) {
            params = objects
        }

        void before(Closure closure) {
            this.before = closure
        }

        void after(Closure closure) {
            this.after = closure
        }

        void replace(Closure closure) {
            this.replace = closure
        }

        void priority(int priority) {
            this.priority = priority
        }

        Object[] getParams() {
            if (params == null) {
                params = []
            }
            return params
        }
    }

    /**
     * Helper class for holding closure
     */
    private static final class BeforeAfterHook {
        private final Object[] args;
        private Object result = UNDEFINED;
        private Throwable throwable;

        private BeforeAfterHook(Object[] methodParameters) {
            this.args = methodParameters
        }
    }

    private static final class GroovyMethodReplacement extends XC_MethodReplacement {
        private MethodHook hook

        private GroovyMethodReplacement(MethodHook hook) {
            super(hook.priority)
            this.hook = hook
        }

        @Override
        protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            def currentHook = new BeforeAfterHook(param.args)
            try {
                rehydrateAndRun(hook.replace, currentHook, param)
                applyThrowable(currentHook, param)
            } catch (Throwable throwable) {
                param.setThrowable(throwable)
            }
            return currentHook.result
        }
    }
    /**
     * A XC_MethodHook implementation
     */
    private static final class GroovyMethodHook extends XC_MethodHook {
        private MethodHook hook

        private GroovyMethodHook(MethodHook hook) {
            this.hook = hook
        }

        @Override
        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            def currentHook = new BeforeAfterHook(param.args)
            try {
                rehydrateAndRun(hook.before, currentHook, param)
                applyResultAndThrowable(currentHook, param)
            } catch (Throwable throwable) {
                param.setThrowable(throwable)
            }
        }


        @Override
        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            def currentHook = new BeforeAfterHook(param.args)
            try {
                def closure = hook.after
                rehydrateAndRun(closure, currentHook, param)
                applyResultAndThrowable(currentHook, param)
            } catch (Throwable throwable) {
                param.setThrowable(throwable)
                Log.e("Penis", "Shits giggles", throwable)
            }
        }

    }

    private static void applyThrowable(BeforeAfterHook currentHook, XC_MethodHook.MethodHookParam param) {
        if (currentHook.throwable != null) {
            param.throwable = currentHook.throwable
        }
    }

    private static void applyResultAndThrowable(BeforeAfterHook currentHook, XC_MethodHook.MethodHookParam param) {
        if (currentHook.result != UNDEFINED) {
            param.result = currentHook.result
        }
        applyThrowable(currentHook, param)
    }

    private
    static void rehydrateAndRun(Closure closure, BeforeAfterHook currentHook, XC_MethodHook.MethodHookParam param) {
        def code = closure?.rehydrate(currentHook, currentHook, param.thisObject)
        code?.resolveStrategy = Closure.DELEGATE_ONLY
        code?.call(*param.args)

    }

    abstract void handleLoadedPackage()

    void handleInitPackageResources() {
    }
}
