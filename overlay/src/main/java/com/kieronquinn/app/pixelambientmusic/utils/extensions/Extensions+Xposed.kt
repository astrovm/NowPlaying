package com.kieronquinn.app.pixelambientmusic.utils.extensions

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import java.lang.reflect.*

fun XC_MethodHook(beforeHookedMethod: ((param: XC_MethodHook.MethodHookParam) -> Unit)? = null, afterHookedMethod: ((param: XC_MethodHook.MethodHookParam) -> Unit)? = null): XC_MethodHook {
    return object: XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            beforeHookedMethod?.invoke(param)
            super.beforeHookedMethod(param)
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            afterHookedMethod?.invoke(param)
            super.afterHookedMethod(param)
        }
    }
}

fun XC_MethodReplacement(replaceHookedMethod: ((param: XC_MethodHook.MethodHookParam) -> Any?)): XC_MethodReplacement {
    return object: XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam): Any? {
            return replaceHookedMethod.invoke(param)
        }
    }
}

/** Resolve the original caller across LSPlant and Pine bridge frames. */
internal fun getCallingClassName(stack: Array<StackTraceElement>): String? {
    val bridge = stack.indexOfLast {
        it.className == "LSPHooker_" || it.className.startsWith("top.canyie.pine.entry.")
    }
    if(bridge < 0) return null
    return stack.drop(bridge + 1).firstOrNull {
        !it.className.startsWith("java.lang.reflect.") &&
                !it.className.startsWith("java.lang.invoke.")
    }?.className
}

fun getCallingClassName(): String? = getCallingClassName(Thread.currentThread().stackTrace)
