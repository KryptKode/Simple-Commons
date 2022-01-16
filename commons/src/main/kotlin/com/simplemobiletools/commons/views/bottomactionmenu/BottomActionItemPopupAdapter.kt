package com.simplemobiletools.commons.views.bottomactionmenu

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.applyColorFilter
import kotlinx.android.synthetic.main.item_action_mode_popup.view.cab_item

class BottomActionItemPopupAdapter(
    private val onSelect: (BottomActionMenuItem) -> Unit,
) : ListAdapter<BottomActionMenuItem, BottomActionItemPopupAdapter.BottomActionItemPopupViewHolder>(DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomActionItemPopupViewHolder {
        return BottomActionItemPopupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_action_mode_popup, parent, false))
    }

    override fun onBindViewHolder(holder: BottomActionItemPopupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BottomActionItemPopupViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: BottomActionMenuItem) {
            view.cab_item.text = item.title
            if (item.icon != View.NO_ID) {
                val icon = ContextCompat.getDrawable(view.context, item.icon)
                icon?.applyColorFilter(Color.WHITE)
                view.cab_item.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            }

            view.setOnClickListener {
                onSelect.invoke(item)
            }
        }
    }

    companion object {
        private val DIFF_UTIL = object : DiffUtil.ItemCallback<BottomActionMenuItem>() {
            override fun areContentsTheSame(oldItem: BottomActionMenuItem, newItem: BottomActionMenuItem): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: BottomActionMenuItem, newItem: BottomActionMenuItem): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}
