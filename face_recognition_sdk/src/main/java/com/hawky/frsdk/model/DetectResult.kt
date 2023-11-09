package com.hawky.frsdk.model

// min face [100, 100]
const val MIN_FACE_WIDTH = 100
const val MIN_FACE_HEIGHT = 100

// max face [800, 800]
const val MAX_FACE_WIDTH = 800
const val MAX_FACE_HEIGHT = 800

// max photo [960, 800]
const val MAX_PHOTO_WIDTH = 960
const val MAX_PHOTO_HEIGHT = 800

// Face sharpness threshold
const val THRESHOLD_FACE_SHARPNESS = 22

// photo limit size
const val LIMIT_PHOTO_SIZE = 1024 * 1024

// min face center distance
const val MIN_FACE_CENTER_DISTANCE = 10

// face stats count
const val FACE_STATS_COUNT = 3

/**
 * Detection result
 */
class DetectResult(
    var status: Int = NOT_FACE,
    var sharpness: Double = 0.0,
    var rects: ArrayList<DetectRect> = ArrayList()
) {
    companion object {
        const val OK = 0
        const val NOT_FACE = -1
        const val NOT_MOUTH = -2
        const val MULTI_FACE = -3
        const val NOT_CLEAR = -4
    }
}