package com.tangem.domain.swap.models

import com.tangem.domain.swap.models.SwapDirection.Direct
import com.tangem.domain.swap.models.SwapDirection.Reverse

/**
 * Swap direction
 *
 * Initial currency can be swap to or from.
 * If swap being swap from direction is [Direct],
 * Otherwise direction is [Reverse]
 */
enum class SwapDirection {
    Direct,
    Reverse,
    ;

    companion object {
        inline fun <T> SwapDirection.withSwapDirection(onDirect: () -> T, onReverse: () -> T): T = when (this) {
            Direct -> onDirect()
            Reverse -> onReverse()
        }
    }
}