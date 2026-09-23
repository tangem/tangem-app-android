package com.tangem.features.polymarket.impl.main.model.converter

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.res.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.features.polymarket.impl.common.formatPolymarketMoneyStyled
import com.tangem.features.polymarket.impl.common.funding.RequisitesState
import com.tangem.features.polymarket.impl.main.model.PredictionBalanceMode
import com.tangem.features.polymarket.impl.main.ui.state.PredictionBalanceUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class PredictionBalanceUMConverter(
    private val onAddFundsClick: () -> Unit,
    private val onWithdrawClick: () -> Unit,
    private val onBalanceModeClick: () -> Unit,
) : Converter<PredictionBalanceUMConverter.Input, PredictionBalanceUM> {

    override fun convert(value: Input): PredictionBalanceUM {
        val status = value.status
        val active = status as? PredictionAccountStatusValue.Active
        val isLoading = status is PredictionAccountStatusValue.Loading
        val totalBalance = active?.totalBalance
        val mode = if (totalBalance == null) PredictionBalanceMode.AVAILABLE else value.mode

        return PredictionBalanceUM(
            balance = when (mode) {
                PredictionBalanceMode.AVAILABLE -> active?.balance
                PredictionBalanceMode.TOTAL -> totalBalance
            }.formatPolymarketMoneyStyled(
                fractionStyle = { TangemTheme.typography3.heading.medium.toSpanStyle() },
            ),
            label = when (mode) {
                PredictionBalanceMode.AVAILABLE -> resourceReference(id = R.string.prediction_main_available_balance)
                PredictionBalanceMode.TOTAL -> resourceReference(id = R.string.prediction_main_total_balance)
            },
            isSwitchEnabled = totalBalance != null,
            onSwitchClick = onBalanceModeClick,
            addFunds = actionButton(
                text = resourceReference(id = R.string.common_add_funds),
                icon = Icons.ic_arrow_down_24,
                isEnabled = value.isTradingAllowed && value.requisites.allowsAddFunds(active),
                isLoading = isLoading,
                isDimmed = value.requisites is RequisitesState.Failed,
                onClick = onAddFundsClick,
            ),
            withdraw = actionButton(
                text = resourceReference(id = R.string.prediction_main_withdraw),
                icon = Icons.ic_arrow_up_24,
                isEnabled = active != null && active.balance > BigDecimal.ZERO,
                isLoading = isLoading,
                onClick = onWithdrawClick,
            ),
        )
    }

    private fun RequisitesState.allowsAddFunds(active: PredictionAccountStatusValue.Active?): Boolean = when (this) {
        RequisitesState.Loading -> false
        RequisitesState.Failed -> active != null
        is RequisitesState.Ready -> active?.cryptoCurrencyStatus != null
    }

    private fun actionButton(
        text: TextReference,
        icon: ImageVector,
        isEnabled: Boolean,
        isLoading: Boolean,
        isDimmed: Boolean = false,
        onClick: () -> Unit,
    ) = TangemButtonUM(
        text = text,
        tangemIconUM = TangemIconUM.Icon(
            imageVector = icon,
            tintReference = { TangemTheme.colors3.icon.primary },
        ),
        type = TangemButtonType.Secondary,
        isEnabled = isEnabled,
        isDimmed = isDimmed,
        isLoading = isLoading,
        onClick = onClick,
    )

    data class Input(
        val status: PredictionAccountStatusValue,
        val requisites: RequisitesState,
        val isTradingAllowed: Boolean,
        val mode: PredictionBalanceMode,
    )
}