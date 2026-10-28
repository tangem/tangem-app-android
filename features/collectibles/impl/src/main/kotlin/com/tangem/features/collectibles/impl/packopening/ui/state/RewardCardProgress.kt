package com.tangem.features.collectibles.impl.packopening.ui.state

internal data class RewardCardProgress(
    val grow: Float,
    val settle: Float,
    val spin: Float,
    val appear: Float = 1f,
    val chrome: Float = 1f,
) {

    companion object {

        val Settled = RewardCardProgress(grow = 1f, settle = 1f, spin = 1f, appear = 1f, chrome = 1f)
    }
}