package com.hawky.fr.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.hawky.frsdk.utils.DimensionUtil
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

/**
 * 自定义一个 AppCompatImageView
 */
class OptionImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    AppCompatImageView(
        context,
        attrs,
        defStyleAttr
    ) {

    init {
        appContext = context
    }

    companion object {
        private lateinit var appContext: Context

        /**
         * 加载本地图片
         *
         * 自定义属性：使用@BindingAdapter
         * 方法必须为公共静态方法，可以有一到多个参数。
         * requireAll = false：可以使用这两个属性中的任一个或同时使用.
         * 注意：方法的第一个参数必须是 OptionImageView 本身
         */
        @BindingAdapter(value = ["resourceId", "isCircle", "radius"], requireAll = false)
        @JvmStatic
        fun setImageSrc(
            view: OptionImageView,
            resourceId: Int?,
            isCircle: Boolean = false,
            radius: Int = 0
        ) {
            // 也可以 load 网络图片 imgUrl
            val builder = Glide.with(view).load(resourceId)
            if (isCircle) {
                builder.transform(CircleCrop())
            } else if (radius > 0) {
                builder.transform(
                    RoundedCornersTransformation(
                        DimensionUtil.dp2px(
                            appContext,
                            radius
                        ), 0
                    )
                )
            }
            val layoutParams = view.layoutParams
            layoutParams?.let {// 设置图片尺寸
                if (it.width > 0 && it.height > 0) {
                    builder.override(it.width, it.height)
                }
            }
            builder.into(view)
        }
    }

}