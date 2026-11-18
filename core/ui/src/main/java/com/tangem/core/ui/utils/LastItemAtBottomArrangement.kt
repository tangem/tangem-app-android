package com.tangem.core.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.Density

object LastItemAtBottomArrangement : Arrangement.Vertical {

    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        var currentOffset = 0
        sizes.forEachIndexed { index, size ->
            outPositions[index] = if (index == sizes.lastIndex) {
                maxOf(currentOffset, totalSize - size)
            } else {
                currentOffset
            }
            currentOffset += size
        }
    }
}