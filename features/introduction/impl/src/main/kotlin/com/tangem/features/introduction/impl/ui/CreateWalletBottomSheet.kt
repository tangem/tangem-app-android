package com.tangem.features.introduction.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.introduction.impl.ui.state.CreateWalletUM
import kotlinx.coroutines.delay

/**
 * How long the sheet's exit stays visible. [TangemModal] keeps the card composed until its spring has fully settled,
 * which is far past the point anything is left to see, and reports nothing in between — so the visible part of the
 * collapse is timed here. The card rides a critically damped `StiffnessMedium` spring (ω = 20 rad/s), which covers
 * ~96% of the way down in this window; the few pixels and the scrim alpha left over are not worth waiting for.
 */
private const val COLLAPSE_ANIMATION_MS = 250L

/**
 * @param config `null` until the viewer first asks for the sheet — the content is the tap's own product. The sheet
 *   is composed anyway, on a placeholder config, so that [prewarm] has something to warm.
 * @param prewarm whether the modal may take its warm-up pass; see `rememberSheetPrewarmReady`.
 * @param onCollapseEnd reported once a dismissed sheet has finished folding away.
 */
@Composable
internal fun CreateWalletBottomSheet(config: TangemBottomSheetConfig?, prewarm: Boolean, onCollapseEnd: () -> Unit) {
    val isCreatingWallet = (config?.content as? CreateWalletUM)?.isCreatingWallet == true

    if (config != null && !config.isShown) {
        LaunchedEffect(config.isShown) {
            delay(COLLAPSE_ANIMATION_MS)
            onCollapseEnd()
        }
    }

    TangemModal<CreateWalletUM>(
        config = config ?: TangemBottomSheetConfig.Empty,
        dismissOnClickOutside = !isCreatingWallet,
        // The glass material samples the video behind the screen, which washes the card out, so the card
        // stays flat. Kept in the window's own overlay: a dialog window costs half a second of first
        // composition before the sheet can even start animating.
        isMaterialEnabled = false,
        // The overlay composes in the host, outside this screen's forced palette, so the modal forces its own.
        forceDarkTheme = true,
        containerColor = TangemTheme.colors3.bg.secondary,
        showDragIndicator = false,
        // First tap on a fresh process is the tap this screen exists for, so the modal is warmed before it.
        prewarm = prewarm,
        title = { content ->
            TangemModal.TopNavigation(
                title = resourceReference(R.string.introduction_button_create_wallet),
                onClose = if (isCreatingWallet) null else content.onCloseClick,
            )
        },
    ) { content ->
        // The rows bring their own 16 dp of padding, which is the inset the design asks for; the 4 dp on top
        // of it is what the design leaves between them.
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            WalletOptionCard(option = content.cardOrRing)
            WalletOptionCard(option = content.seedPhrase)
        }
    }
}