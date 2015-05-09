package dk.mehmedbasic.groovyxposedbridge

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
    protected static final Object UNDEFINED = new Object() {
        @Override
        String toString() {
            return "GroovyXposed.UNDEFINED"
        }
    }

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
        Object[] typesAndCallback = hook.params + [new GroovyMethodHook(hook)]
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
    static def replaceMethod(Class clazz, Closure closure) {
        MethodHook hook = createHook(closure)
        Object[] typesAndCallback = hook.params + [new GroovyMethodReplacement(hook)]
        findAndHookMethod(clazz, hook.name, typesAndCallback)
    }

    def hookConstructor(String className, Closure closure) {
        hookConstructor(findClass(className), closure)
    }

    static def hookConstructor(Class clazz, Closure closure) {
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
    private static MethodHook createHook(Closure closure) {
        def hook = new MethodHook()
        def code = closure.rehydrate(hook, hook, hook)
        code.resolveStrategy = Closure.DELEGATE_FIRST
        code.call()
        hook
    }

    /**
     * Holding the DSL hook
     */
    private static final class MethodHook {
        private String name;
        private Object[] params = [];

        private Object before = UNDEFINED
        private Object after = UNDEFINED
        private Object replace = UNDEFINED

        private int priority = 50;

        void method(String name) {
            this.name = name
        }

        void params(Object... objects) {
            params = objects
        }

        void before(Object closure) {
            this.before = closure
        }

        void after(Object closure) {
            this.after = closure
        }

        void replace(Object closure) {
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

        Object getBefore() {
            if (before == UNDEFINED) {
                return null
            }
            return before
        }

        Object getAfter() {
            if (after == UNDEFINED) {
                return null
            }
            return after
        }

        Object getReplace() {
            if (replace == UNDEFINED) {
                return null
            }
            return replace
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

            def replace = hook.getReplace()
            if (replace instanceof XC_MethodReplacement) {
                return ((XC_MethodReplacement) replace).replaceHookedMethod(param)
            }

            def code = rehydrate(replace as Closure, currentHook, param)

            code?.call(*param.args)
            apply(currentHook, param)
            return null
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

            def before = hook.getBefore()
            if (before instanceof XC_MethodHook) {
                ((XC_MethodHook) before).beforeHookedMethod(param)
            } else {
                def code = rehydrate(before as Closure, currentHook, param)
                code?.call(*param.args)
                apply(currentHook, param)
            }
        }


        @Override
        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            def currentHook = new BeforeAfterHook(param.args)
            def after = hook.getAfter()
            if (after instanceof XC_MethodHook) {
                ((XC_MethodHook) after).afterHookedMethod(param)
            } else {
                def code = rehydrate(after as Closure, currentHook, param)
                code?.call(*param.args)
                apply(currentHook, param)
            }
        }

    }

    /**
     * A constant closure
     *
     * @param o the return value
     *
     * @return the closure
     */
    static def XC_MethodReplacement returnConstant(Object o) {
        new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                return o
            }
        }
    }


    private static void apply(BeforeAfterHook currentHook, XC_MethodHook.MethodHookParam param) {
        if (currentHook.result != UNDEFINED) {
            param.result = currentHook.result
        }
        if (currentHook.throwable != null) {
            param.throwable = currentHook.throwable
        }
        param.args = currentHook.args
    }

    static Closure rehydrate(Closure closure, BeforeAfterHook currentHook, XC_MethodHook.MethodHookParam param) {
        def code = closure?.rehydrate(currentHook, currentHook, param.thisObject)
        code?.resolveStrategy = Closure.DELEGATE_ONLY
        return code ?: null
    }

    abstract void handleLoadedPackage()

    void handleInitPackageResources() {
    }

    private static final class ConstantReturn extends XC_MethodReplacement {

        @Override
        protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
            return null
        }
    }
}
