package com.hawky.frsdk.core

import android.content.Context
import android.graphics.Bitmap
import com.hawky.frsdk.model.DetectResult

/**
 * 检测人脸的接口
 */
interface IFaceDetect {
    fun init(context: Context, callback: (Boolean) -> Unit)
    fun detectFaces(bmp: Bitmap): DetectResult
}