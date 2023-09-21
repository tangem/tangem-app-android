package com.tangem.feature.wallet.presentation.wallet.state.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.feature.wallet.impl.R

/**
 * Wallet bottom sheet config
 *
[REDACTED_AUTHOR]
 */
sealed class WalletBottomSheetConfig(
    open val title: TextReference,
    open val subtitle: TextReference,
    @DrawableRes open val iconResId: Int,
    open val tint: Color? = null,
    val primaryButtonConfig: ButtonConfig,
    val secondaryButtonConfig: ButtonConfig,
) : TangemBottomSheetConfigContent {

    data class ButtonConfig(
        val text: TextReference,
        val onClick: () -> Unit,
        @DrawableRes val iconResId: Int? = null,
    )

    data class UnlockWallets(val onUnlockClick: () -> Unit, val onScanClick: () -> Unit) : WalletBottomSheetConfig(
        title = resourceReference(id = R.string.common_unlock_needed),
        subtitle = resourceReference(
            id = R.string.unlock_wallet_description_full,
            formatArgs = wrappedList(
                resourceReference(R.string.common_biometrics),
            ),
        ),
        iconResId = R.drawable.ic_locked_24,
        tint = TangemColorPalette.Black,
        primaryButtonConfig = ButtonConfig(
            text = resourceReference(id = R.string.user_wallet_list_unlock_all),
            onClick = onUnlockClick,
        ),
        secondaryButtonConfig = ButtonConfig(
            text = resourceReference(id = R.string.welcome_unlock_card),
            onClick = onScanClick,
            iconResId = R.drawable.ic_tangem_24,
        ),
    )
}