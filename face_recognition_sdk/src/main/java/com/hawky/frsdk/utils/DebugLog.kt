package com.hawky.frsdk.utils

import android.util.Log
import org.opencv.BuildConfig

object DebugLog {
    private var className: String? = null
    private var methodName: String? = null
    private var lineNumber = 0
    private val isDebuggable = BuildConfig.DEBUG

    private fun createLog(log: String): String {
        val buffer = StringBuffer()
        buffer.append("fr-sdk")
        buffer.append("[")
        buffer.append(methodName)
        buffer.append(":")
        buffer.append(lineNumber)
        buffer.append("]")
        buffer.append(log)
        return buffer.toString()
    }

    private fun getMethodNames(sElements: Array<StackTraceElement>) {
        className = sElements[1].fileName
        methodName = sElements[1].methodName
        lineNumber = sElements[1].lineNumber
    }

    fun e(message: String) {
        if (!isDebuggable) return
        getMethodNames(Throwable().stackTrace)
        Log.e(className, createLog(message))
    }

    fun i(message: String) {
        if (!isDebuggable) return
        getMethodNames(Throwable().stackTrace)
        Log.i(className, createLog(message))
    }

    fun d(message: String) {
        if (!isDebuggable) return
        getMethodNames(Throwable().stackTrace)
        Log.d(className, createLog(message))
    }

    fun v(message: String) {
        if (!isDebuggable) return
        getMethodNames(Throwable().stackTrace)
        Log.v(className, createLog(message))
    }

    fun w(message: String) {
        if (!isDebuggable) return
        getMethodNames(Throwable().stackTrace)
        Log.w(className, createLog(message))
    }

    fun wtf(message: String) {
        if (!isDebuggable) return
        getMethodNames(Throwable().stackTrace)
        Log.wtf(className, createLog(message))
    }
}