@file:Suppress("MagicNumber")

package com.tangem.features.earn.flow.impl.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.amountScreen.preview.AmountScreenClickIntentsStub
import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.predefined.PredefinedPercentButtonUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.button.TangemButtonIconPosition
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.earn.api.ui.EarnRowUM
import com.tangem.features.earn.flow.impl.ui.state.EarnAmountUM
import com.tangem.features.earn.flow.impl.ui.state.EarnConfirmUM
import com.tangem.features.earn.flow.impl.ui.state.EarnIconsUM
import com.tangem.features.earn.flow.impl.ui.state.EarnLegalUM
import com.tangem.features.earn.flow.impl.ui.state.EarnResultUM
import com.tangem.features.earn.product.api.session.EarnResultAsset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// region Preview
@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnAmountContentPreview(@PreviewParameter(EarnPreviewProvider::class) preview: EarnPreview) {
    TangemThemePreviewRedesign {
        EarnAmountContent(state = preview.amount(), feeBlock = null, onBack = {}, onClose = {})
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnConfirmContentPreview(@PreviewParameter(EarnPreviewProvider::class) preview: EarnPreview) {
    TangemThemePreviewRedesign {
        EarnConfirmContent(
            state = preview.confirm(),
            title = preview.preset.productTitle,
            feeBlock = null,
            onBack = {},
            onClose = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnResultContentPreview(@PreviewParameter(EarnPreviewProvider::class) preview: EarnPreview) {
    TangemThemePreviewRedesign {
        EarnResultContent(state = preview.result())
    }
}

internal enum class EarnPreviewPreset(val productTitle: TextReference, val providerIcon: Int) {
    Lending(productTitle = stringReference("Lending on Aave"), providerIcon = R.drawable.ic_aave_36),
    Vault(productTitle = stringReference("High Yield Vault"), providerIcon = R.drawable.img_aave_22),
}

internal enum class EarnPreviewAction { Deposit, Withdraw }

internal data class EarnPreview(val preset: EarnPreviewPreset, val action: EarnPreviewAction) {

    private val isWithdraw = action == EarnPreviewAction.Withdraw

    private val icons = EarnIconsUM(
        currency = CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.img_eth_22,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
        provider = ImageReference.Res(preset.providerIcon),
    )

    private val notice: TextReference? = when {
        !isWithdraw -> null
        preset == EarnPreviewPreset.Lending ->
            stringReference("Your funds will be available for use immediately after withdrawal.")
        else -> stringReference("Withdrawals settle within 7 days, the funds arrive after that.")
    }

    private val actionLabel = stringReference(if (isWithdraw) "Withdraw" else "Deposit")

    fun amount() = EarnAmountUM(
        title = if (isWithdraw) stringReference("Withdrawal") else stringReference("Deposit to Lending on Aave"),
        amountState = AmountStatePreviewData.amountStateV2,
        clickIntents = AmountScreenClickIntentsStub,
        percents = if (isWithdraw) {
            persistentListOf()
        } else {
            listOf(25, 50, 75, 100).map { percent ->
                PredefinedPercentButtonUM(id = "$percent", label = stringReference("$percent%"), onClick = {})
            }.toImmutableList()
        },
        notice = notice,
        notifications = persistentListOf(),
        primary = TangemButtonUM(text = actionLabel, type = TangemButtonType.Primary, onClick = {}),
        onHowItWorksClick = {},
        isFromPosition = isWithdraw,
    )

    fun confirm() = EarnConfirmUM(
        icons = icons,
        amountFiat = stringReference("$800"),
        amountCrypto = stringReference("0.5 ETH"),
        rows = if (isWithdraw) persistentListOf() else depositRows(),
        notice = notice,
        notifications = persistentListOf(),
        legal = EarnLegalUM(termsOfUseUrl = "", privacyPolicyUrl = "", onClick = {}),
        stepLabel = null,
        primary = TangemButtonUM(
            text = actionLabel,
            type = TangemButtonType.Primary,
            tangemIconUM = TangemIconUM.Icon(Icons.ic_logo_tangem_24),
            iconPosition = TangemButtonIconPosition.End,
            onClick = {},
        ),
    )

    fun result(): EarnResultUM = if (isWithdraw) withdrawResult() else depositResult()

    private fun depositResult() = EarnResultUM(
        icons = icons,
        asset = EarnResultAsset.Logos,
        variant = TangemStatusScreen.Variant.Success,
        title = stringReference("Deposited 0.5 ETH"),
        subtitle = stringReference("Your ETH in Main account is now earning in Aave"),
        chip = stringReference("Current 5.5% APY"),
        rows = persistentListOf(),
        primary = TangemButtonUM(text = stringReference("Go to yield"), type = TangemButtonType.Primary, onClick = {}),
        secondary = TangemButtonUM(
            text = stringReference("Go to token"),
            type = TangemButtonType.Secondary,
            onClick = {},
        ),
        explorer = null,
        onClose = {},
    )

    private fun withdrawResult() = EarnResultUM(
        icons = icons,
        asset = EarnResultAsset.Status(TangemStatusIcon.Kind.Checkmark),
        variant = if (preset == EarnPreviewPreset.Vault) {
            TangemStatusScreen.Variant.Info
        } else {
            TangemStatusScreen.Variant.Success
        },
        title = stringReference(if (preset == EarnPreviewPreset.Vault) "Withdrawal requested" else "Withdrawn 0.5 ETH"),
        subtitle = stringReference("$2,130.88 · 2 May 2024 at 9:41"),
        chip = null,
        rows = persistentListOf(
            row(
                label = "Withdrawal period",
                value = if (preset == EarnPreviewPreset.Vault) "Up to 7 days" else "Usually instant",
            ),
            row(label = "Network fee", value = "$1.40"),
        ),
        primary = null,
        secondary = null,
        explorer = EarnResultUM.ExplorerUM(onExploreClick = {}, onShareClick = {}),
        onClose = {},
    )

    private fun depositRows(): ImmutableList<EarnRowUM> = persistentListOf(
        row(label = "Current APY", value = if (preset == EarnPreviewPreset.Vault) "7.2%" else "5.5%"),
        row(label = "Performance fee", value = "15%"),
        row(label = "Projected earnings", value = "~$20,414/year"),
    )

    private fun row(label: String, value: String) =
        EarnRowUM(label = stringReference(label), value = stringReference(value), onInfoClick = null)
}

internal class EarnPreviewProvider : PreviewParameterProvider<EarnPreview> {
    override val values: Sequence<EarnPreview>
        get() = EarnPreviewPreset.entries.asSequence().flatMap { preset ->
            EarnPreviewAction.entries.asSequence().map { action -> EarnPreview(preset, action) }
        }
}
// endregion