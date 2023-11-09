package com.hawky.frsdk.biz

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.lifecycle.ViewModel
import com.hawky.frsdk.core.OpenCVFaceDetect
import com.hawky.frsdk.model.DetectRect
import com.hawky.frsdk.model.DetectResult
import com.hawky.frsdk.model.FACE_STATS_COUNT
import com.hawky.frsdk.model.MAX_FACE_HEIGHT
import com.hawky.frsdk.model.MAX_FACE_WIDTH
import com.hawky.frsdk.model.MAX_PHOTO_HEIGHT
import com.hawky.frsdk.model.MAX_PHOTO_WIDTH
import com.hawky.frsdk.model.MIN_FACE_CENTER_DISTANCE
import com.hawky.frsdk.model.MIN_FACE_HEIGHT
import com.hawky.frsdk.model.MIN_FACE_WIDTH
import com.hawky.frsdk.utils.DebugLog
import com.hawky.frsdk.utils.ImageUtil
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * 处理相机预览图
 */
class CameraDetectViewModel(
    private val context: Application,
    private val callback: (CameraDetectResult) -> Unit
) :
    ViewModel() {
    private val executor = Executors.newSingleThreadExecutor()
    private val isRunning = AtomicBoolean(false)
    private var preDetectRect: DetectRect? = null
    private var faceCount = 0
//    private val cacheBitmapSharp =
//        object : LinkedHashMap<Bitmap, Double>(FACE_STATS_COUNT, 1.0f, true) {
//            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Bitmap, Double>?): Boolean {
//                return size > FACE_STATS_COUNT
//            }
//        }

    enum class DetectStatus {
        FAILED, SUCCESS, PROGRESS
    }

    class CameraDetectResult {
        var status: DetectStatus = DetectStatus.FAILED
        var msg = ""
        var progress = 0
        var faceBitmap: String? = null
    }

    fun actionCameraFace(
        data: ByteArray,
        degrees: Float,
        previewWidth: Int,
        previewHeight: Int,
        isFontCamera: Boolean,
    ) {
        if (!isRunning.get()) {
            executor.submit(
                FaceDetectRunnable(
                    data,
                    degrees,
                    previewWidth,
                    previewHeight,
                    isFontCamera
                )
            )
        }
    }

    private inner class FaceDetectRunnable    // preview frame
        (
        private val data: ByteArray,
        private val previewAngle: Float,
        private val previewWidth: Int,
        private val previewHeight: Int,
        private val isFontCamera: Boolean,
    ) :
        Runnable {
        override fun run() {
            isRunning.set(true)
            DebugLog.d("FaceDetectRunnable start-----${Thread.currentThread().id}-----")
            // 1.nv21 (Preview format YUV) to rgb
            val sourceBitmap = ImageUtil.NV21toRGB565(previewWidth, previewHeight, data)
                ?: return DebugLog.d("sourceBitmap is null")
            // 2.Handles front and rear camera rotation angles
            val matrix = Matrix()
            matrix.postRotate(previewAngle)
            // 3.Horizontal image flip
            if (isFontCamera) matrix.postScale(-1f, 1f)
            var destBitmap = Bitmap.createBitmap(
                sourceBitmap,
                0,
                0,
                sourceBitmap.width,
                sourceBitmap.height,
                matrix,
                false
            )
            if (sourceBitmap != destBitmap) {
                sourceBitmap.recycle()
            }
            val width = destBitmap.width.toFloat()
            val height = destBitmap.height.toFloat()
            if (width > MAX_PHOTO_WIDTH || height > MAX_PHOTO_HEIGHT) {
                val scaleX = MAX_PHOTO_WIDTH / width
                val scaleY = MAX_PHOTO_HEIGHT / height
                val resizeScale = scaleX.coerceAtMost(scaleY)
                val resizedBitmap = Bitmap.createScaledBitmap(
                    destBitmap,
                    (width * resizeScale).roundToInt(),
                    (height * resizeScale).roundToInt(), true
                )
                if (destBitmap != resizedBitmap) {
                    destBitmap.recycle()
                }
                destBitmap = resizedBitmap
            }

            // 4.detect face
            val faceResult = OpenCVFaceDetect.detectFaces(destBitmap)
            val status = faceResult.status
            DebugLog.d("faceResult.status : $status")
            val detectResult = handleDetectResult(faceResult, destBitmap)
            callback.invoke(detectResult)
            destBitmap.recycle()
            isRunning.set(false)
            DebugLog.d("FaceDetectRunnable end-----${Thread.currentThread().id}-----")
        }

        private fun handleDetectResult(
            faceResult: DetectResult,
            destBitmap: Bitmap
        ): CameraDetectResult {
            if (faceResult.status != DetectResult.OK) faceCount = 0// reset
            val detectResult = CameraDetectResult()
            when (faceResult.status) {
                DetectResult.NOT_FACE -> {
                    detectResult.status = DetectStatus.FAILED
                    detectResult.msg = "未检测到人脸"
                }

                DetectResult.NOT_MOUTH, DetectResult.MULTI_FACE -> {
                    detectResult.status = DetectStatus.FAILED
                    detectResult.msg = "请着屏幕，勿遮挡面部"
                }

                DetectResult.NOT_CLEAR -> {
                    detectResult.msg = "面部不够清晰"
                }

                DetectResult.OK -> {
                    detectResult.status = DetectStatus.PROGRESS
                    detectResult.msg = "检测到人脸..."
                    evaluateFace(destBitmap, detectResult, faceResult.rects)
                }
            }
            return detectResult
        }

        private fun evaluateFace(
            destBitmap: Bitmap,
            result: CameraDetectResult,
            rects: List<DetectRect>
        ) {
            for (i in rects.indices) {
                val currRect = rects[i]
                val rect = currRect.rect
                val width = rect.width()
                val height = rect.height()
                DebugLog.d("rects size w,h=$width,$height")
                if ((width in MIN_FACE_WIDTH..MAX_FACE_WIDTH) && (height in MIN_FACE_HEIGHT..MAX_FACE_HEIGHT)) {
                    var tempRect = preDetectRect
                    if (tempRect != null) {
                        val distance = Math.sqrt(
                            Math.pow(Math.abs(currRect.mid.x - tempRect.mid.x).toDouble(), 2.0) +
                                    Math.pow(
                                        Math.abs(currRect.mid.y - tempRect.mid.y).toDouble(),
                                        2.0
                                    )
                        )
                        if (distance <= MIN_FACE_CENTER_DISTANCE) {
                            result.status = DetectStatus.FAILED
                            result.msg = "请左右稍微摇头"
                            faceCount = 0
                        } else faceCount++
                        DebugLog.d("calculateFace#faceCount:${faceCount},distance:$distance")
                    } else {
                        tempRect = DetectRect()
                        faceCount++
                        DebugLog.d("calculateFace#faceCount:${faceCount}")
                    }

                    // update DetectRect obj
                    tempRect.id = faceCount
                    tempRect.mid = currRect.mid
                    tempRect.rect = currRect.rect
                    tempRect.time = currRect.time
                    preDetectRect = tempRect

                    result.progress = faceCount * 360 / FACE_STATS_COUNT
                    if (faceCount == FACE_STATS_COUNT) {
                        DebugLog.d("calculateFace#detect ok.")
                        val savePath = "face-${currRect.time}.jpg"
                        val jpgFileName = ImageUtil.saveBitmapToFile(context, destBitmap, savePath)
                        result.status = DetectStatus.SUCCESS
                        result.msg = "人脸检测成功"
                        result.faceBitmap = jpgFileName
                    }
                }
            }
        }
    }

//    private fun findMaxSharpnessBitmap(): Bitmap? {
//        val max = cacheBitmapSharp.entries.stream().max(Map.Entry.comparingByValue())
//        val entry = max.orElse(null) ?: return null
//        return entry.key
//    }

    fun release() {
        try {
            executor.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}