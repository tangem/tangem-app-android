package com.tangem.features.welcome.impl.ui.state

import com.tangem.core.ui.components.artwork.ArtworkUM
import com.tangem.core.ui.extensions.TextReference
import javax.annotation.concurrent.Immutable

internal data class WalletUM(
    val name: TextReference,
    val subtitle: TextReference,
    val imageState: ImageState,
    val onClick: () -> Unit,
) {

    @Immutable
    sealed class ImageState {
        data object MobileWallet : ImageState()
        data object Loading : ImageState()
        data class Image(
            val artwork: ArtworkUM,
        ) : ImageState()
    }
}