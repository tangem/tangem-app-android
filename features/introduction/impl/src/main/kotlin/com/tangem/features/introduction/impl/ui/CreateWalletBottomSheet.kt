package com.tangem.features.introduction.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.introduction.impl.ui.state.CreateWalletUM

@Composable
internal fun CreateWalletBottomSheet(config: TangemBottomSheetConfig) {
    val isCreatingWallet = (config.content as? CreateWalletUM)?.isCreatingWallet == true
    TangemModal<CreateWalletUM>(
        config = config,
        dismissOnClickOutside = !isCreatingWallet,
        // The glass material samples the video behind the screen, which washes the card out and leaves a
        // sheen across its top. A dialog window cannot blur across windows, so the card stays flat and opaque.
        presentation = TangemModal.Presentation.Dialog,
        title = { content ->
            TangemTopNavigation(
                title = resourceReference(R.string.introduction_button_create_wallet),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0.dp),
                fadeEnabled = false,
                onBack = null,
                onClose = if (isCreatingWallet) null else content.onCloseClick,
            )
        },
    ) { content ->
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WalletOptionCard(option = content.cardOrRing)
            WalletOptionCard(option = content.seedPhrase)
        }
    }
}