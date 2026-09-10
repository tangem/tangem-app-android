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
import com.tangem.features.polymarket.impl.main.model.RequisitesState
import com.tangem.features.polymarket.impl.main.ui.state.PredictionBalanceUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class PredictionBalanceUMConverter(
    private val onAddFundsClick: () -> Unit,
    private val onWithdrawClick: () -> Unit,
) : Converter<PredictionBalanceUMConverter.Input, PredictionBalanceUM> {

    override fun convert(value: Input): PredictionBalanceUM {
        val status = value.status
        val active = status as? PredictionAccountStatusValue.Active
        val isLoading = status is PredictionAccountStatusValue.Loading

        return PredictionBalanceUM(
            balance = active?.balance.formatPolymarketMoneyStyled(
                fractionStyle = { TangemTheme.typography3.heading.medium.toSpanStyle() },
            ),
            addFunds = actionButton(
                text = resourceReference(id = R.string.common_add_funds),
                icon = Icons.ic_arrow_down_24,
                isEnabled = active?.cryptoCurrencyStatus != null &&
                    value.isTradingAllowed &&
                    value.requisites !is RequisitesState.Loading,
                isLoading = isLoading,
                onClick = onAddFundsClick,
            ).copy(isDimmed = value.requisites is RequisitesState.Failed),
            withdraw = actionButton(
                text = resourceReference(id = R.string.prediction_main_withdraw),
                icon = Icons.ic_arrow_up_24,
                isEnabled = active != null && active.balance > BigDecimal.ZERO,
                isLoading = isLoading,
                onClick = onWithdrawClick,
            ),
        )
    }

    private fun actionButton(
        text: TextReference,
        icon: ImageVector,
        isEnabled: Boolean,
        isLoading: Boolean,
        onClick: () -> Unit,
    ) = TangemButtonUM(
        text = text,
        tangemIconUM = TangemIconUM.Icon(
            imageVector = icon,
            tintReference = { TangemTheme.colors3.icon.primary },
        ),
        type = TangemButtonType.Secondary,
        isEnabled = isEnabled,
        isLoading = isLoading,
        onClick = onClick,
    )

    data class Input(
        val status: PredictionAccountStatusValue,
        val requisites: RequisitesState,
        val isTradingAllowed: Boolean,
    )
}