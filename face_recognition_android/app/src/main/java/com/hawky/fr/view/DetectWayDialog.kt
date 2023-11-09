package com.hawky.fr.view

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hawky.fr.R
import com.hawky.fr.databinding.ItemDetectWayBinding
import com.hawky.fr.model.DetectWayItem
import com.hawky.fr.model.TYPE_ALBUM
import com.hawky.fr.model.TYPE_CAMERA
import com.hawky.frsdk.utils.DimensionUtil

/**
 * 检测方式选择框
 */
class DetectWayDialog(private val context: Context, private val callback: (DetectWayItem) -> Unit) :
    AlertDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        val root = FrameLayout(context)
        root.setBackgroundColor(Color.WHITE)

        val recyclerView = RecyclerView(context)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = DialogAdapter(loadInitialData())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val margin = DimensionUtil.dp2px(context, 25)
        params.leftMargin = margin
        params.topMargin = margin
        params.rightMargin = margin
        params.bottomMargin = margin
        params.gravity = Gravity.CENTER

        root.addView(recyclerView, params)

        setCancelable(false)
        setContentView(root)
    }

    private fun loadInitialData(): List<DetectWayItem> {
        return ArrayList<DetectWayItem>().apply {
            add(DetectWayItem(TYPE_ALBUM).apply {
                icon = R.mipmap.ic_album
                desc = context.getString(R.string.fr_way_album)
            })
            add(DetectWayItem(TYPE_CAMERA).apply {
                icon = R.mipmap.ic_camera
                desc = context.getString(R.string.fr_way_camera)
            })
        }
    }

    private inner class DialogAdapter(private var itemList: List<DetectWayItem>) :
        RecyclerView.Adapter<DialogAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View, val binding: ItemDetectWayBinding) :
            RecyclerView.ViewHolder(itemView) {
            fun bindData(item: DetectWayItem) {
                binding.item = item
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDetectWayBinding.inflate(LayoutInflater.from(context), parent, false)
            return ViewHolder(binding.root, binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = itemList[position]
            holder.bindData(item)
            holder.itemView.setOnClickListener {
                dismiss()
                callback.invoke(item)
            }
        }

        override fun getItemCount(): Int {
            return itemList.size
        }
    }

}