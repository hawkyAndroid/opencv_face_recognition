package com.hawky.frsdk.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import com.hawky.frsdk.model.DetectRect
import com.hawky.frsdk.model.DetectResult
import com.hawky.frsdk.model.THRESHOLD_FACE_SHARPNESS
import com.hawky.frsdk.utils.DebugLog
import org.opencv.R
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 用OpenCV检测人脸
 *
 * https://opencv.org/releases/
 */
object OpenCVFaceDetect : IFaceDetect {
    private var faceCascadeClassifier: CascadeClassifier? = null
    private var mouthCascadeClassifier: CascadeClassifier? = null

    @Volatile
    private var initial: Boolean = false

    override fun init(context: Context, callback: (Boolean) -> Unit) {
        if (initial) return
        initial = true
        val loaderCallback = object : BaseLoaderCallback(context) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    SUCCESS -> {
                        DebugLog.d("OpenCV loaded successfully")

                        // load facial cascade classifier
                        faceCascadeClassifier = loadCascadeClassifier(
                            context,
                            R.raw.haarcascade_frontalface_alt,
                            "haarcascade_frontalface_alt.xml"
                        )
                        // load mouth cascade classifier
                        mouthCascadeClassifier = loadCascadeClassifier(
                            context,
                            R.raw.haarcascade_mcs_mouth,
                            "haarcascade_mcs_mouth.xml"
                        )
                        // More --> https://github.com/opencv/opencv/tree/master/data/haarcascades
                        callback(true)
                    }

                    else -> {
                        super.onManagerConnected(status)
                        callback(false)// Negligible
                    }
                }
            }
        }

        if (!OpenCVLoader.initDebug()) {
            DebugLog.d("Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, context, loaderCallback);
        } else {
            DebugLog.d("OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private fun loadCascadeClassifier(
        context: Context,
        rawId: Int,
        xmlName: String
    ): CascadeClassifier? {
        var classifier: CascadeClassifier? = null
        try {
            // load cascade file from application resources
            context.resources.openRawResource(rawId)
                .use { input ->
                    val cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE)
                    val cascadeFile = File(cascadeDir, xmlName)

                    FileOutputStream(cascadeFile).use { os ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            os.write(buffer, 0, bytesRead)
                        }
                    }

                    val cascadeClassifier = CascadeClassifier(cascadeFile.absolutePath)
                    classifier = if (cascadeClassifier.empty()) {
                        DebugLog.e("Failed to load cascade classifier")
                        null
                    } else {
                        DebugLog.d("Loaded cascade classifier from " + cascadeFile.absolutePath)
                        cascadeClassifier
                    }

                    cascadeDir.delete()
                }
        } catch (e: IOException) {
            DebugLog.e("Failed to load cascade. Exception thrown: ${e.localizedMessage}")
        }

        return classifier
    }

    override fun detectFaces(bmp: Bitmap): DetectResult {
        val detectResult = DetectResult()

        // bitmap --> srcMat
        val srcMat = Mat(bmp.height, bmp.width, CvType.CV_8UC3)
        Utils.bitmapToMat(bmp, srcMat)
        // srcMat --> dstMat(Gray)
        val dstMat = Mat(bmp.height, bmp.width, CvType.CV_8UC1)
        Imgproc.cvtColor(srcMat, dstMat, Imgproc.COLOR_BGR2GRAY)

        // Detect faces
        val faceObjects = MatOfRect()
        faceCascadeClassifier?.detectMultiScale(
            dstMat,
            faceObjects,
            1.1,
            8,
            0,
            Size(60.0, 60.0),
            Size()
        )
        val faces = faceObjects.toArray()
        DebugLog.d("Count of detected faces:${faces.size}")
        if (faces.size > 1) {
            detectResult.status = DetectResult.MULTI_FACE
            return detectResult
        }

        if (faces.isNotEmpty()) {// If faces exists, then check mouth
            val face = faces[0]
            DebugLog.d("detected face w,h=" + face.width + "," + face.height)
            // Lower half of the face as mouth rect
            val halROI = dstMat.submat(
                Rect(
                    face.x,
                    face.y + face.height / 2,
                    face.width,
                    face.height / 2
                )
            )

            // Detect mouths
            val mouthObjects = MatOfRect()
            mouthCascadeClassifier?.detectMultiScale(
                halROI,
                mouthObjects,
                1.1,
                12,
                0,
                Size(30.0, 30.0),
                Size()
            )
            val mouths = mouthObjects.toArray()
            DebugLog.d("Number of detected mouths:${mouths.size}")

            if (mouths.isNotEmpty()) {
                // 检测面部清晰度
                val faceROI: Mat = dstMat.submat(face)
                val bitmap =
                    Bitmap.createBitmap(faceROI.width(), faceROI.height(), Bitmap.Config.RGB_565)
                Utils.matToBitmap(faceROI, bitmap)
                val sharpness = calculateFaceSharpness(bitmap)
                DebugLog.d("face sharpness:$sharpness")
                if (sharpness > THRESHOLD_FACE_SHARPNESS) {
                    detectResult.status = DetectResult.OK
                    detectResult.sharpness = sharpness
                    val detectRect = DetectRect()
                    val rect = android.graphics.Rect(
                        face.x,
                        face.y,
                        face.x + face.width,
                        face.y + face.height
                    )
                    detectRect.rect = rect
                    detectRect.mid = Point(
                        face.x + face.width / 2,
                        face.y + face.height / 2
                    )
                    detectResult.rects.add(detectRect)
                } else {
                    detectResult.status = DetectResult.NOT_CLEAR
                }
            } else {
                detectResult.status = DetectResult.NOT_MOUTH
            }
        } else {
            detectResult.status = DetectResult.NOT_FACE
        }

        return detectResult
    }

    private fun calculateFaceSharpness(bitmap: Bitmap): Double {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        // 梯度计算
        val gradientX = Mat()
        val gradientY = Mat()
        Imgproc.Sobel(mat, gradientX, CvType.CV_64F, 1, 0)
        Imgproc.Sobel(mat, gradientY, CvType.CV_64F, 0, 1)
        // 计算梯度振幅和对比度
        val gradientMagnitude = Mat()
        Core.magnitude(gradientX, gradientY, gradientMagnitude)
        return Core.mean(gradientMagnitude).`val`[0]
    }

}