package com.hawky.fr

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.hawky.fr.databinding.ActivityMainBinding
import com.hawky.fr.model.TYPE_ALBUM
import com.hawky.fr.model.TYPE_CAMERA
import com.hawky.fr.view.DetectWayDialog
import com.hawky.fr.view.LoadingDialog
import com.hawky.frsdk.biz.AlbumFaceDetection
import com.hawky.frsdk.core.OpenCVFaceDetect
import com.hawky.frsdk.model.MAX_PHOTO_HEIGHT
import com.hawky.frsdk.model.MAX_PHOTO_WIDTH
import com.hawky.frsdk.utils.DebugLog
import com.hawky.frsdk.utils.ImageUtil
import com.yalantis.ucrop.UCrop
import java.io.File


class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private var detectWayDialog: DetectWayDialog? = null
    private var progressDialog: ProgressDialog? = null
    private var albumFaceDetection: AlbumFaceDetection? = null
    private var loadingDialog: LoadingDialog? = null
    private var croppedImageUri: Uri? = null
    private var opencvInitSuccess: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        loadingDialog = LoadingDialog(this, false)

        OpenCVFaceDetect.init(this) { opencvInitSuccess = it }
    }

    fun selectDetectWay(view: View) {
        var dialog = detectWayDialog
        if (dialog == null) {
            dialog = DetectWayDialog(this) {
                when (it.option) {
                    TYPE_ALBUM -> actionAccessAlbum()
                    TYPE_CAMERA -> {
                        clearAlbumDetection()
                        actionAccessCamera()
                    }
                }
            }
        }
        detectWayDialog = dialog
        dialog.show()
    }

    private fun actionAccessAlbum() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            choosePhotoFromAlbum()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun actionAccessCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCameraActivity()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_CAMERA
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                choosePhotoFromAlbum()
            } else {
                Toast.makeText(this, "Please open Album access permissions.", Toast.LENGTH_SHORT)
                    .show()
            }
        } else if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraActivity()
            } else {
                Toast.makeText(this, "Please open Camera access permissions.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    /**
     * 相册检测人脸
     */
    private fun choosePhotoFromAlbum() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_ALBUM)
    }

    /**
     * 相机检测人脸
     */
    private fun startCameraActivity() {
        val intent = Intent(this, CameraDetectActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_ALBUM && resultCode == RESULT_OK && data != null) {
            if (!opencvInitSuccess) {
                Toast.makeText(this, getString(R.string.fr_init_failed), Toast.LENGTH_SHORT).show()
                return
            }
            val uri = data.data ?: return DebugLog.d("Picked Photo Uri is null")
            val path: String = ImageUtil.convertPathFromUri(application, uri)
                ?: return DebugLog.d("Picked Photo path is null")
            val bitmap: Bitmap =
                ImageUtil.getBitmapByPath(path) ?: return DebugLog.d("Picked Photo bitmap is null")
            val width = bitmap.width
            val height = bitmap.height
            DebugLog.d("Picked Photo w,h=$width,$height")
            startCropActivity(uri)// val aspectRatio = width > MAX_PHOTO_WIDTH || height > MAX_PHOTO_HEIGHT
        } else if (requestCode == REQUEST_CROP_IMAGE && resultCode == RESULT_OK) {// 裁剪
            // 裁剪完成，可以在这里处理裁剪后的图像
            DebugLog.d("Cropped Image Uri: $croppedImageUri")
            showTipDialog("检测中...")
            actionPickedAlbum(croppedImageUri)
        } else if (requestCode == REQUEST_CODE_CAMERA && data != null) {// 相机
            if (resultCode == RESULT_OK) {
                val path = data.getStringExtra("image_path")
                binding.ivShowFace.setImageBitmap(BitmapFactory.decodeFile(path))
            }
        }
    }

    private fun startCropActivity(sourceUri: Uri) {
        // 设置裁剪后保存的路径
        val cropUri = Uri.fromFile(File(cacheDir, "cropped_image.jpg"))
        // 创建 UCrop.Options 对象并设置最大裁剪框尺寸
        val options = UCrop.Options()
        options.setStatusBarColor(Color.WHITE)
        options.setHideBottomControls(true)
        options.setToolbarTitle("")
        cropUri?.let {
            UCrop.of(sourceUri, it)
//        if (aspectRatio) withAspectRatio(3f, 2f)
                .withMaxResultSize(MAX_PHOTO_WIDTH, MAX_PHOTO_HEIGHT)
                .withOptions(options) // 应用设置的选项
                .start(this, REQUEST_CROP_IMAGE)
        }
        croppedImageUri = cropUri
    }

    private fun actionPickedAlbum(uri: Uri?) {
        var tempDetection = albumFaceDetection
        if (tempDetection == null) {
            tempDetection =
                AlbumFaceDetection(application) { success, error, fileName ->
                    runOnUiThread {
                        hideTipDialog()
                        showDetectResult(error)
                        if (success) {
                            binding.ivShowFace.setImageBitmap(BitmapFactory.decodeFile(fileName))
                        }
                    }
                }
        }
        albumFaceDetection = tempDetection
        tempDetection.handlePhotoUri(uri)
    }

    private fun clearAlbumDetection() {
        albumFaceDetection?.release()
        albumFaceDetection = null
    }

    private fun showDetectResult(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("OK", null)
            .create()
            .show()
    }

    private fun showTipDialog(message: String) {
        var dialog = progressDialog
        if (dialog == null) {
            dialog = ProgressDialog(this)
            dialog.setCancelable(false)
            dialog.isIndeterminate = false
        }
        dialog.setMessage(message)
        progressDialog = dialog
        if (dialog.isShowing) return
        dialog.show()
    }

    private fun hideTipDialog() {
        progressDialog?.let { if (it.isShowing) it.dismiss() }
    }

    override fun onDestroy() {
        super.onDestroy()
        clearAlbumDetection()
        loadingDialog?.let { if (it.isShowing) it.dismiss() }
    }

    private companion object {
        const val PERMISSION_CAMERA = 1
        const val PERMISSION_READ_EXTERNAL_STORAGE = 2
        const val REQUEST_CODE_PICK_ALBUM = 3
        const val REQUEST_CODE_CAMERA = 4
        const val REQUEST_CROP_IMAGE = 103
    }

}