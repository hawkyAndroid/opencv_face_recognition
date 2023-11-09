package com.hawky.fr.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.hawky.fr.BR
import java.io.Serializable

const val TYPE_ALBUM = 1
const val TYPE_CAMERA = 2

data class DetectWayItem(
    var option: Int,
) : Serializable, BaseObservable() {

    @Bindable
    var icon: Int = -1
        set(value) {
            field = value
            notifyPropertyChanged(BR._all)
        }

    @Bindable
    var desc: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR._all)
        }
}