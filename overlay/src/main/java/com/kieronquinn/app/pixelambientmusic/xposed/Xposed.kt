package com.kieronquinn.app.pixelambientmusic.xposed

import com.kieronquinn.app.pixelambientmusic.xposed.Xposed.MethodHookParam
import java.lang.reflect.Member
import java.lang.reflect.Method
import com.kieronquinn.app.pixelambientmusic.utils.pine.XC_MethodHook as PineXC_MethodHook
import com.kieronquinn.app.pixelambientmusic.utils.pine.XC_MethodHook.MethodHookParam as PineMethodHookParam
import com.kieronquinn.app.pixelambientmusic.utils.pine.XC_MethodReplacement as PineXC_MethodReplacement
import com.kieronquinn.app.pixelambientmusic.utils.pine.XposedBridge as PineXposedBridge
import de.robv.android.xposed.XC_MethodHook as HookXC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam as HookMethodHookParam
import de.robv.android.xposed.XC_MethodReplacement as HookXC_MethodReplacement
import de.robv.android.xposed.XposedBridge as HookXposedBridge

object Xposed {

    private var _USE_PINE: Boolean? = null

    private val USE_PINE: Boolean
        get() = _USE_PINE ?: shouldUsePine()

    fun hookMethod(replace: Member, hook: MethodHook) {
        if(USE_PINE) {
            PineXposedBridge.hookMethod(replace, object: PineXC_MethodHook() {
                override fun beforeHookedMethod(param: PineMethodHookParam) {
                    val localParam = MethodHookParam(param.thisObject, param.args, param.result)
                    return hook.beforeHookedMethod(localParam).also {
                        if(localParam.returnEarly) {
                            param.result = localParam.result
                        }
                    }
                }

                override fun afterHookedMethod(param: PineMethodHookParam) {
                    val localParam = MethodHookParam(param.thisObject, param.args, param.result)
                    return hook.afterHookedMethod(localParam).also {
                        if(localParam.returnEarly) {
                            param.result = localParam.result
                        }
                    }
                }
            })
        }else{
            HookXposedBridge.hookMethod(replace, object: HookXC_MethodHook() {
                override fun beforeHookedMethod(param: HookMethodHookParam) {
                    val localParam = MethodHookParam(param.thisObject, param.args, param.result)
                    return hook.beforeHookedMethod(localParam).also {
                        if(localParam.returnEarly) {
                            param.result = localParam.result
                        }
                    }
                }

                override fun afterHookedMethod(param: HookMethodHookParam) {
                    val localParam = MethodHookParam(param.thisObject, param.args, param.result)
                    return hook.afterHookedMethod(localParam).also {
                        if(localParam.returnEarly) {
                            param.result = localParam.result
                        }
                    }
                }
            })
        }
    }

    fun replaceMethod(replace: Member, replacement: MethodReplacement) {
        if(USE_PINE) {
            PineXposedBridge.hookMethod(replace, object: PineXC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    val localParam = MethodHookParam(param.thisObject, param.args, param.result)
                    return replacement.replaceHookedMethod(localParam).also {
                        param.result = localParam.result
                    }
                }
            })
        }else{
            HookXposedBridge.hookMethod(replace, object: HookXC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    val localParam = MethodHookParam(param.thisObject, param.args, param.result)
                    return replacement.replaceHookedMethod(localParam).also {
                        param.result = localParam.result
                    }
                }
            })
        }
    }

    fun deoptimizeMethod(method: Method) {
        if(USE_PINE) {
            PineXposedBridge.deoptimizeMethod(method)
        }else{
            HookXposedBridge.deoptimizeMethod(method)
        }
    }

    /**
     *  Check if LSPlant is compatible with this device; if not, try to use Pine. If that fails, the
     *  app will crash due to no supported hooking methods.
     */
    @Synchronized
    private fun shouldUsePine(): Boolean {
        return try {
            HookXposedBridge()
            false
        }catch (e: UnsatisfiedLinkError) {
            true
        }.also {
            _USE_PINE = it
        }
    }

    abstract class MethodHook {
        open fun beforeHookedMethod(param: MethodHookParam) {}
        open fun afterHookedMethod(param: MethodHookParam) {}
    }

    abstract class MethodReplacement {
        open fun replaceHookedMethod(param: MethodHookParam): Any? {
            return param.result
        }
    }

    class MethodHookParam(
        val thisObject: Any?,
        val args: Array<Any?>,
        var _result: Any? = null
    ) {
        var returnEarly: Boolean = false
        var result: Any?
            set(value) {
                _result = value
                returnEarly = true
            }
            get() = _result
    }
}