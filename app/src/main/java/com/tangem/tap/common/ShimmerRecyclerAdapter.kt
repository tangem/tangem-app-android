package com.tangem.tap.common

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
* [REDACTED_AUTHOR]
 */
open class ShimmerRecyclerAdapter(
    private val viewHolderViewFactory: (ViewGroup) -> ViewGroup,
) : ListAdapter<ShimmerData, ShimmerVH>(DiffUtilCallback) {

    override fun getItemId(position: Int): Long {
        return if (currentList.isEmpty()) 0 else currentList[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmerVH {
        return ShimmerVH(viewHolderViewFactory.invoke(parent))
    }

    override fun onBindViewHolder(holder: ShimmerVH, position: Int) {}

    object DiffUtilCallback : DiffUtil.ItemCallback<ShimmerData>() {
        override fun areContentsTheSame(oldItem: ShimmerData, newItem: ShimmerData) = oldItem == newItem
        override fun areItemsTheSame(oldItem: ShimmerData, newItem: ShimmerData) = oldItem == newItem
    }
}

class ShimmerVH(viewGroup: ViewGroup) : RecyclerView.ViewHolder(viewGroup)

data class ShimmerData(private val any: String = "")
