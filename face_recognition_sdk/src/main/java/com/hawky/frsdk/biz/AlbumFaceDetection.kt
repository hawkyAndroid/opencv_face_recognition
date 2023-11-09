package com.hawky.frsdk.biz

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.hawky.frsdk.core.IFaceDetect
import com.hawky.frsdk.core.OpenCVFaceDetect
import com.hawky.frsdk.model.DetectResult
import com.hawky.frsdk.model.LIMIT_PHOTO_SIZE
import com.hawky.frsdk.model.MAX_FACE_HEIGHT
import com.hawky.frsdk.model.MAX_FACE_WIDTH
import com.hawky.frsdk.model.MIN_FACE_HEIGHT
import com.hawky.frsdk.model.MIN_FACE_WIDTH
import com.hawky.frsdk.utils.DebugLog
import com.hawky.frsdk.utils.ImageUtil
import org.opencv.R

/**
 * 检测相册人脸
 */
class AlbumFaceDetection(
    private val context: Application,
    private val callback: (Boolean, String, String?) -> Unit
) {
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    init {
        init()
    }

    private fun init() {
        val thread = HandlerThread("PhotoFaceDetection")
        thread.start()

        handler = object : Handler(thread.looper) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_PHOTO_URI) {
                    val uri = msg.obj as Uri
                    val path = ImageUtil.convertPathFromUri(context, uri) ?: return callback.invoke(
                        false,
                        context.getString(R.string.fr_invalid_photo_path),
                        null
                    )
                    DebugLog.d("uri:$uri, path:$path")
                    val rotateBitmap = ImageUtil.getBitmapByPath(path) ?: return
                    val detectResult = OpenCVFaceDetect.detectFaces(rotateBitmap)
                    DebugLog.d("detectResult status=${detectResult.status}")
                    if (detectResult.status == DetectResult.OK) {
                        var rectTime = 0L
                        var validFace = false
                        val rects = detectResult.rects
                        DebugLog.d("rects.size=${rects.size}")
                        for (i in rects.indices) { // filter face size
                            val detectRect = rects[i]
                            val rect = rects[i].rect
                            val width = rect.width()
                            val height = rect.height()
                            DebugLog.d("detectRect.size w=$width,h=$height")
                            if ((width in MIN_FACE_WIDTH..MAX_FACE_WIDTH) && (height in MIN_FACE_HEIGHT..MAX_FACE_HEIGHT)) {
                                rectTime = detectRect.time
                                validFace = true
                                break
                            }
                        }
                        if (validFace) {
                            val savePath = "face-$rectTime.jpg"
                            val compressBitmap: Bitmap =
                                ImageUtil.compressBitmap(rotateBitmap, LIMIT_PHOTO_SIZE)
                                    ?: return DebugLog.d("compressBitmap is null")

                            val jpgFileName =
                                ImageUtil.saveBitmapToFile(context, compressBitmap, savePath)
                                    ?: return DebugLog.d("jpgFileName is null")
                            callback.invoke(
                                true,
                                context.getString(R.string.fr_detect_ok),
                                jpgFileName
                            )
                        } else {
                            callback.invoke(
                                false,
                                context.getString(R.string.fr_invalid_size),
                                null
                            )
                        }
                    } else if (detectResult.status == DetectResult.NOT_CLEAR) {
                        callback.invoke(
                            false,
                            context.getString(R.string.fr_not_clear_enough),
                            null
                        )
                    } else {
                        callback.invoke(false, context.getString(R.string.fr_invalid_photo), null)
                    }
                }
            }
        }

        handlerThread = thread
    }

    fun handlePhotoUri(uri: Uri?) {
        handler?.let { it.sendMessage(it.obtainMessage(MSG_PHOTO_URI, uri)) }
    }

    fun release() {
        handlerThread?.quit()
        handlerThread = null
        handler = null
    }

    private companion object {
        const val MSG_PHOTO_URI = 1
    }
}