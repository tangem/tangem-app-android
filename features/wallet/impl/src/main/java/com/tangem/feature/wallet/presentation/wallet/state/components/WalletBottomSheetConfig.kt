package com.tangem.feature.wallet.presentation.wallet.state.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.feature.wallet.impl.R

/**
 * Wallet bottom sheet config
 *
 * @property isShow           flag that determine if bottom sheet is shown
 * @property onDismissRequest lambda be invoked when bottom sheet is dismissed
 * @property content          content config
 *
[REDACTED_AUTHOR]
 */
// TODO: Finalize notification strings [REDACTED_JIRA]
internal data class WalletBottomSheetConfig(
    val isShow: Boolean,
    val onDismissRequest: () -> Unit,
    val content: BottomSheetContentConfig,
) {

    sealed class BottomSheetContentConfig(
        open val title: TextReference,
        open val subtitle: TextReference,
        @DrawableRes open val iconResId: Int,
        open val tint: Color? = null,
        val primaryButtonConfig: ButtonConfig,
        val secondaryButtonConfig: ButtonConfig,
    ) {

        data class ButtonConfig(
            val text: TextReference,
            val onClick: () -> Unit,
            @DrawableRes val iconResId: Int? = null,
        )

        data class UnlockWallets(
            val onUnlockClick: () -> Unit,
            val onScanClick: () -> Unit,
        ) : BottomSheetContentConfig(
            title = TextReference.Str(value = "Unlock needed"),
            subtitle = TextReference.Str(
                value = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                    "incididunt ut labore et dolore magna aliqua.",
            ),
            iconResId = R.drawable.ic_locked_24,
            tint = TangemColorPalette.Black,
            primaryButtonConfig = ButtonConfig(text = TextReference.Str(value = "Unlock"), onClick = onUnlockClick),
            secondaryButtonConfig = ButtonConfig(
                text = TextReference.Str(value = "Scan card"),
                onClick = onScanClick,
                iconResId = R.drawable.ic_tangem_24,
            ),
        )

        data class LikeTangemApp(
            val onRateTheAppClick: () -> Unit,
            val onShareClick: () -> Unit,
        ) : BottomSheetContentConfig(
            title = TextReference.Str(value = "Like Tangem App?"),
            subtitle = TextReference.Str(value = "How was your experience with our app? Let us know:"),
            iconResId = R.drawable.ic_star_24,
            tint = TangemColorPalette.Tangerine,
            primaryButtonConfig = ButtonConfig(
                text = TextReference.Str(value = "Rate the app"),
                onClick = onRateTheAppClick,
            ),
            secondaryButtonConfig = ButtonConfig(
                text = TextReference.Str(value = "Share feedback"),
                onClick = onShareClick,
            ),
        )

        data class CriticalWarningAlreadySignedHashes(
            val onOkClick: () -> Unit,
            val onCancelClick: () -> Unit,
        ) : BottomSheetContentConfig(
            title = TextReference.Res(
                id = R.string.warning_important_security_info,
                formatArgs = WrappedList(listOf("\u26A0")),
            ),
            subtitle = TextReference.Res(id = R.string.alert_signed_hashes_message),
            iconResId = R.drawable.img_attention_20,
            tint = null,
            primaryButtonConfig = ButtonConfig(
                text = TextReference.Res(id = R.string.common_ok),
                onClick = onOkClick,
            ),
            secondaryButtonConfig = ButtonConfig(
                text = TextReference.Res(id = R.string.common_cancel),
                onClick = onCancelClick,
            ),
        )
    }
}