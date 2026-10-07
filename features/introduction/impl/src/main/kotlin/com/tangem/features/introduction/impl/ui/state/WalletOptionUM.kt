package com.tangem.features.introduction.impl.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.extensions.TextReference

internal data class WalletOptionUM(
    val icon: Icon,
    val title: TextReference,
    val description: TextReference,
    val isEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val onClick: () -> Unit,
) {

    sealed interface Icon {
        /** A single-color DS icon, tinted with the theme's primary icon color */
        data class Tinted(val vector: ImageVector) : Icon

        /** A brand logo drawn in its own colors */
        data class Image(@DrawableRes val res: Int) : Icon
    }
}