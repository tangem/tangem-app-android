package com.tangem.tap.common.recyclerView

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.tangem.sdk.extensions.dpToPx

class SpaceItemDecoration(
    private val horizontalSpaceDp: Float,
    private val verticalSpaceDp: Float,
) : RecyclerView.ItemDecoration() {

    private lateinit var space: Space

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (state.itemCount == 0) return
        if (!::space.isInitialized) {
            space = Space(
                view.dpToPx(horizontalSpaceDp).toInt(),
                view.dpToPx(verticalSpaceDp).toInt(),
            )
        }

        outRect.left = space.horizontal
        outRect.right = space.horizontal

        when (state.itemCount) {
            1 -> {
                outRect.top = space.vertical
                outRect.bottom = space.vertical
            }
            else -> {
                val adapterPosition = parent.getChildAdapterPosition(view)
                if (adapterPosition == -1) return

                when (adapterPosition) {
                    0 -> {
                        // first
                        outRect.top = space.vertical
                        outRect.bottom = space.vertical / 2
                    }
                    state.itemCount - 1 -> {
                        // last
                        outRect.top = space.vertical / 2
                        outRect.bottom = space.vertical
                    }
                    else -> {
                        // middle
                        outRect.top = space.vertical / 2
                        outRect.bottom = space.vertical / 2
                    }
                }
            }
        }
    }

    private data class Space(
        val horizontal: Int,
        val vertical: Int,
    )

    companion object {
        fun all(dp: Float): SpaceItemDecoration = SpaceItemDecoration(dp, dp)
        fun vertical(dp: Float): SpaceItemDecoration = SpaceItemDecoration(0f, dp)
        fun horizontal(dp: Float): SpaceItemDecoration = SpaceItemDecoration(dp, 0f)
    }
}
