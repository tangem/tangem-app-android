package com.tangem.features.send.v2.feeselector

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.utils.getFiatString
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.EMPTY_BALANCE_SIGN
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.entity.FeeFiatRateUM
import com.tangem.features.send.v2.feeselector.entity.FeeItem
import com.tangem.features.send.v2.feeselector.entity.FeeSelectorUM
import com.tangem.features.send.v2.feeselector.entity.PrimaryButtonConfig
import com.tangem.features.send.v2.feeselector.model.FeeSelectorModel
import com.tangem.features.send.v2.impl.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal class DefaultFeeSelectorBlockComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: FeeSelectorParams.FeeSelectorBlockParams,
) : FeeSelectorBlockComponent, AppComponentContext by appComponentContext {

    private val model: FeeSelectorModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        FeeSelectorBlockContent(modifier = modifier, state = state)
    }

    @AssistedFactory
    interface Factory : FeeSelectorBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: FeeSelectorParams.FeeSelectorBlockParams,
        ): DefaultFeeSelectorBlockComponent
    }
}

@Composable
private fun FeeSelectorBlockContent(state: FeeSelectorUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(TangemTheme.colors.background.action)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.ic_fee_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
            text = stringResourceSafe(R.string.common_network_fee_title),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        Icon(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing6)
                .size(TangemTheme.dimens.size16),
            painter = painterResource(id = R.drawable.ic_token_info_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
        SpacerWMax()
        when (state) {
            is FeeSelectorUM.Content -> FeeContent(state)
            is FeeSelectorUM.Loading -> FeeLoading()
            is FeeSelectorUM.Error -> FeeError()
        }
    }
}

@Composable
private fun RowScope.FeeError() {
    Text(
        text = EMPTY_BALANCE_SIGN,
        color = TangemTheme.colors.text.primary1,
        style = TangemTheme.typography.body2,
    )
}

@Composable
private fun RowScope.FeeLoading() {
    TextShimmer(
        radius = TangemTheme.dimens.radius3,
        style = TangemTheme.typography.body1,
        modifier = Modifier.width(width = TangemTheme.dimens.size90),
    )
}

@Composable
private fun RowScope.FeeContent(state: FeeSelectorUM.Content) {
    EllipsisText(
        text = if (state.feeFiatRateUM != null) {
            getFiatString(
                value = state.selectedFeeItem.fee.amount.value,
                rate = state.feeFiatRateUM.rate,
                appCurrency = state.feeFiatRateUM.appCurrency,
                approximate = state.isFeeApproximate,
            )
        } else {
            state.selectedFeeItem.fee.amount.value.format {
                crypto(
                    symbol = state.selectedFeeItem.fee.amount.currencySymbol,
                    decimals = state.selectedFeeItem.fee.amount.decimals,
                ).fee(canBeLower = state.isFeeApproximate)
            }
        },
        style = TangemTheme.typography.body1,
        color = TangemTheme.colors.text.tertiary,
        textAlign = TextAlign.End,
        modifier = Modifier
            .weight(1f)
            .padding(start = TangemTheme.dimens.spacing4),
    )
    Icon(
        modifier = Modifier.size(width = 18.dp, height = 24.dp),
        painter = painterResource(id = R.drawable.ic_select_18_24),
        contentDescription = null,
        tint = TangemTheme.colors.icon.informative,
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeeSelectorBlockContent_Preview() {
    TangemThemePreview {
        val feeItem = FeeItem.Market(
            Fee.Common(amount = Amount(value = BigDecimal("0.0002876"), blockchain = Blockchain.Ethereum)),
        )
        FeeSelectorBlockContent(
            modifier = Modifier.fillMaxWidth(),
            state = FeeSelectorUM.Content(
                doneButtonConfig = PrimaryButtonConfig(enabled = true, onClick = {}),
                feeItems = persistentListOf(feeItem),
                onFeeSelected = {},
                selectedFeeItem = feeItem,
                isFeeApproximate = false,
                feeFiatRateUM = FeeFiatRateUM(
                    rate = BigDecimal("2500"),
                    appCurrency = AppCurrency.Default,
                ),
                displayNonceInput = false,
                nonce = null,
                onNonceChange = {},
            ),
        )
    }
}
