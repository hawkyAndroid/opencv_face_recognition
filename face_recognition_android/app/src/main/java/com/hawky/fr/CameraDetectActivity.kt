package com.hawky.fr

import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.hawky.fr.databinding.ActivityCameraDetectBinding
import com.hawky.frsdk.biz.CameraDetectViewModel
import com.hawky.frsdk.utils.CameraUtil
import com.hawky.frsdk.utils.DebugLog
import java.io.File

/**
 * 相机检测人脸页
 */
class CameraDetectActivity : BaseActivity(), SurfaceHolder.Callback, Camera.PreviewCallback {
    private lateinit var binding: ActivityCameraDetectBinding
    private lateinit var cameraDetectViewModel: CameraDetectViewModel
    private var mCamera: Camera? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var fontCameraId = 0
    private var backCameraId = 0
    private var currentCameraId = 0
    private var previewWidth = 0
    private var previewHeight = 0
    private var previewAngle = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera_detect)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initData()
        initCamera()
        setListener()
    }

    private fun initData() {
        cameraDetectViewModel = CameraDetectViewModel(application) {
            DebugLog.d("${it.msg},${it.progress}")
            binding.txtTip.text = it.msg
            binding.faceCircle.setProgress(it.progress)
            if (it.status == CameraDetectViewModel.DetectStatus.SUCCESS) {
                val path = it.faceBitmap
                if (path.isNullOrEmpty()) setResult(RESULT_CANCELED) else {
                    val file = File(path)
                    if (file.exists()) setResult(
                        RESULT_OK,
                        Intent().putExtra("image_path", path)
                    ) else setResult(RESULT_CANCELED)
                }
                finish()
            }
        }
    }

    private fun setListener() {
        binding.btSwitchCamera.setOnClickListener { switchCamera() }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val holder = binding.cameraView.holder
        // 监听 SurfaceView 生命周期
        holder.addCallback(this)
        // 相机预览的图像格式为 NV21（YUV）格式
        holder.setFormat(ImageFormat.NV21)
    }

    /**
     * 切换摄像头
     */
    private fun switchCamera() {
        currentCameraId = if (currentCameraId == fontCameraId) backCameraId else fontCameraId
        closeCamera()
        openCamera()
        configureCamera(surfaceWidth, surfaceHeight)
        startPreview()
    }

    /**
     * 当 SurfaceView 被创建时调用
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        openCamera()
    }

    /**
     * 当 SurfaceView 的大小或格式发生变化时调用
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        DebugLog.d("surfaceChanged")
        // No surface, return immediately
        holder.surface ?: return
        // Try to stop the current preview
        try {
            mCamera?.stopPreview()
        } catch (e: Exception) {
            // Ignore...
        }
        // Configure camera params
        surfaceWidth = width
        surfaceHeight = height
        configureCamera(width, height)
        // Everything is configured! Finally start the camera preview again
        startPreview()
    }

    /**
     * 当 SurfaceView 即将被销毁时调用
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        DebugLog.d("surfaceDestroyed")
        closeCamera()
    }

    /**
     * 接收实时预览帧数据
     */
    override fun onPreviewFrame(data: ByteArray, camera: Camera?) {
        cameraDetectViewModel.actionCameraFace(
            data,
            previewAngle,
            previewWidth,
            previewHeight,
            currentCameraId == fontCameraId
        )
    }

    override fun onResume() {
        super.onResume()
        startPreview()
    }

    override fun onPause() {
        super.onPause()
        pausePreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDetectViewModel.release()
    }

    private fun initCamera() {
        val numberOfCameras = Camera.getNumberOfCameras()
        val cameraInfo = CameraInfo()
        for (i in 0 until numberOfCameras) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                fontCameraId = i
            } else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                backCameraId = i
            }
        }
        // Default to use front camera
        currentCameraId = fontCameraId
    }

    private fun configureCamera(width: Int, height: Int) {
        val parameters = mCamera?.parameters ?: return
        // Set the PreviewSize and AutoFocus
        setOptimalPreviewSize(parameters, width, height)
        setAutoFocus(parameters)
        // And set the parameters
        mCamera?.parameters = parameters
        // Now set the display orientation
        val windowOrientation = CameraUtil.getWindowOrientation(this)
        val cameraOrientation = CameraUtil.getCameraOrientation(windowOrientation, currentCameraId)
        mCamera?.run {
            setDisplayOrientation(cameraOrientation)
            setErrorCallback { error, _ -> DebugLog.d("an unexpected camera error: $error") }
        }

        previewAngle =
            (if (currentCameraId == fontCameraId) cameraOrientation + 180 else cameraOrientation).toFloat()
    }

    private fun setOptimalPreviewSize(
        cameraParameters: Camera.Parameters,
        width: Int,
        height: Int
    ) {
        val previewSizes = cameraParameters.supportedPreviewSizes ?: return

        val targetRatio = height.toDouble() / width
        val previewSize =
            CameraUtil.getOptimalPreviewSize(this, previewSizes, targetRatio) ?: return
        previewWidth = previewSize.width
        previewHeight = previewSize.height
        DebugLog.d("previewWidth=$previewWidth, previewHeight=$previewHeight")

        cameraParameters.setPreviewSize(previewSize.width, previewSize.height)
    }

    private fun setAutoFocus(cameraParameters: Camera.Parameters) {
        val list = cameraParameters.supportedFocusModes
        if (list.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            cameraParameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
    }

    /**
     * 开始预览
     */
    private fun startPreview() {
        mCamera?.let {
            // 启动相机预览
            it.startPreview()
            // 设置PreviewCallback
            it.setPreviewCallback(this)
        }
    }

    /**
     * 停止预览
     */
    private fun pausePreview() {
        mCamera?.let {
            it.setPreviewCallback(null)
            it.stopPreview()
        }
    }

    private fun openCamera() {
        try {
            // 通过相机的 cameraId 来打开相机
            //（相机开启后，可以连续地捕获图像数据并在预览界面上显示这些数据）
            mCamera = Camera.open(currentCameraId)
            // 将相机的预览数据设置到一个 SurfaceView 绘图表面上，
            // 以便在 SurfaceView 上实时显示相机捕捉到的图像。
            mCamera?.setPreviewDisplay(binding.cameraView.holder)
        } catch (e: Exception) {
            DebugLog.e("Could not preview the image:${e.localizedMessage}")
        }
    }

    private fun closeCamera() {
        mCamera?.run {
            // 停止相机预览
            stopPreview()
            // 停止相机预览回调
            setPreviewCallbackWithBuffer(null)
            // 停止相机触发错误回调
            setErrorCallback(null)
            // 释放相机资源（之后 camera 对象不可再用）
            release()
        }
        mCamera = null
    }

}