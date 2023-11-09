package com.hawky.frsdk.model

import android.graphics.Point
import android.graphics.Rect

/**
 * Detection range
 */
data class DetectRect(
    var id: Int = 0,
    // 人脸中心点
    var mid: Point = Point(0, 0),
    // 人脸矩形
    var rect: Rect = Rect(0, 0, 0, 0),
    // 记录时间戳
    var time: Long = System.currentTimeMillis()
)