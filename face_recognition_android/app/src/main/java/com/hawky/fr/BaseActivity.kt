package com.hawky.fr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hawky.frsdk.utils.StatusBar

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FaceRecognition)
        StatusBar.fitSystemBar(this)

        super.onCreate(savedInstanceState)
    }
}