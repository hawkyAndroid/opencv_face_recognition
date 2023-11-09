package com.hawky.frsdk.utils

import android.content.Context

class DimensionUtil {
    companion object {
        @JvmStatic
        fun dp2px(context: Context, dpValue: Int): Int {
            val metrics = context.resources.displayMetrics
            return (metrics.density * dpValue + 0.5f).toInt()
        }
    }
}