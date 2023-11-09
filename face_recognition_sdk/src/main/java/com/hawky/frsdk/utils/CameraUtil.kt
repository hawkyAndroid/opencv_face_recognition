package com.hawky.frsdk.utils

import android.app.Activity
import android.graphics.Point
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.view.Surface

object CameraUtil {

    /**
     * Obtain rotation degrees of window
     */
    fun getWindowOrientation(activity: Activity): Int {
        return when (activity.windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    /**
     * Obtain orientation of camera
     */
    fun getCameraOrientation(degrees: Int, cameraId: Int): Int {
        // See android.hardware.Camera.setDisplayOrientation for
        // documentation.
        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        var result: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        DebugLog.d("cameraId = $cameraId,camera facing = ${info.facing},camera orientation = ${info.orientation}")
        return result
    }

    /**
     * Obtain optimal preview size
     */
    fun getOptimalPreviewSize(
        activity: Activity,
        sizes: List<Camera.Size>?, targetRatio: Double
    ): Camera.Size? {
        // Use a very small tolerance because we want an exact match.
        val ASPECT_TOLERANCE = 0.001
        if (sizes == null) return null
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE
        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        val point = getDefaultDisplaySize(activity, Point())
        val targetHeight = Math.min(point.x, point.y)
        // Try to find an size match aspect ratio and size
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - targetHeight).toDouble()
            }
        }
        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            DebugLog.d("No preview size match the aspect ratio")
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - targetHeight).toDouble()
                }
            }
        }
        return optimalSize
    }

    /**
     * Obtain default display size
     */
    private fun getDefaultDisplaySize(activity: Activity, point: Point): Point {
        activity.windowManager.defaultDisplay.getSize(point)
        return point
    }

}