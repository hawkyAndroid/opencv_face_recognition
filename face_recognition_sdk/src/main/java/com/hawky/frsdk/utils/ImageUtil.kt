package com.hawky.frsdk.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtil {

    /**
     * Convert uri to image path
     */
    fun convertPathFromUri(context: Context, uri: Uri): String? {
        var path = uri.path
        if (!TextUtils.isEmpty(uri.authority)) {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATA),
                null,
                null,
                null
            ) ?: return null

            cursor.moveToFirst()
            val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            path = cursor.getString(index.coerceAtLeast(0))
            cursor.close()
        }
        return path
    }

    /**
     * Obtain bitmap by file path
     */
    fun getBitmapByPath(filePath: String): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        var bitmap = BitmapFactory.decodeFile(filePath, options)
        try {
            val ei = ExifInterface(filePath)
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> bitmap = rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> bitmap = rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> bitmap = rotateBitmap(bitmap, 270f)
            }
        } catch (e: IOException) {
            DebugLog.e("Failed to obtain bitmap. Exception thrown:${e.localizedMessage}")
        }

        return bitmap
    }

    /**
     * Save bitmap to the specified file directory
     */
    fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val fileDir = context.getExternalFilesDir("images") ?: return null
            if (!fileDir.exists()) fileDir.mkdirs()
            val file = File(fileDir, fileName)
            DebugLog.d("file:${file.absolutePath}")

            FileOutputStream(file).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                    bos.flush()
                }
            }

            file.absolutePath
        } catch (e: IOException) {
            DebugLog.e("Failed to save bitmap. Exception thrown:${e.localizedMessage}")
            null
        }
    }

    fun NV21toRGB565(width: Int, height: Int, nv21: ByteArray?): Bitmap? {
        // convert the image from NV21 to RGB_565
        var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val yuv = YuvImage(nv21, ImageFormat.NV21, bitmap!!.width, bitmap.height, null)
        val rectImage = Rect(0, 0, bitmap.width, bitmap.height)
        val outputStream = ByteArrayOutputStream()
        if (!yuv.compressToJpeg(rectImage, 100, outputStream)) {
            DebugLog.d("face image from NV21 to RGB compressToJpeg failed")
        }
        // decode the RGB_565 bitmap
        val bfo = BitmapFactory.Options()
        bfo.inPreferredConfig = Bitmap.Config.RGB_565
        bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(outputStream.toByteArray()), null, bfo)
        return bitmap
    }

    /**
     * Compress bitmap in limit size
     */
    fun compressBitmap(bitmap: Bitmap, limitSize: Int): Bitmap? {
        try {
            ByteArrayOutputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                val initialSize = outputStream.toByteArray().size
                if (initialSize <= limitSize) {
                    return bitmap
                }
                DebugLog.d("Original size: ${initialSize / 1024}KB")

                val scaleFactor =
                    Math.sqrt((limitSize / initialSize.toFloat()).toDouble()).toFloat()
                val matrix = Matrix()
                matrix.setScale(scaleFactor, scaleFactor)
                val compressedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
                bitmap.recycle()

                try {
                    ByteArrayOutputStream().use {
                        compressedBitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            it
                        )
                        val compressedSize = it.toByteArray().size
                        DebugLog.d("Compressed size: ${compressedSize / 1024}KB")
                    }
                } catch (e: IOException) {
                    DebugLog.e("Failed to compress bitmap-1. Exception thrown:${e.localizedMessage}")
                }
                return compressedBitmap
            }
        } catch (e: IOException) {
            DebugLog.e("Failed to compress bitmap-2. Exception thrown:${e.localizedMessage}")
        }

        return bitmap
    }

    /**
     * Rotate bitmap by degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap? {
        var bmp = bitmap
        if (degrees == 0F) return bmp

        val matrix = Matrix()
        matrix.setRotate(
            degrees, bmp.width.toFloat() / 2,
            bmp.height.toFloat() / 2
        )
        val b = Bitmap.createBitmap(
            bmp, 0, 0, bmp.width,
            bmp.height, matrix, true
        ) ?: return null

        if (bmp != b) {
            bmp.recycle()
            bmp = b
        }

        return bmp
    }

}