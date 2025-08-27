package com.tangem.common.ui.amountScreen.models

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import kotlinx.collections.immutable.PersistentList
import java.math.BigDecimal

/** Model for amount state */
@Stable
sealed class AmountState {

    abstract val isPrimaryButtonEnabled: Boolean
    abstract val isRedesignEnabled: Boolean

    /**
     * @param isPrimaryButtonEnabled indicates if next state button enabled
     * @param title title
     * @param availableBalance user crypto currency balance with fiat balance
     * @param availableBalanceShort user crypto currency balance without fiat balance
     * @param tokenIconState crypto currency icon state
     * @param segmentedButtonConfig currency switcher config
     * @param selectedButton selected currency index
     * @param isSegmentedButtonsEnabled indicates if currency switches is enabled
     * @param amountTextField amount field state
     * @param appCurrency app currency
     * @param isEditingDisabled indicated whether amount is editable
     * @param reduceAmountBy reduces amount to be sent by specified value
     * @param isIgnoreReduce ignores reduce amount value
     */
    data class Data(
        override val isPrimaryButtonEnabled: Boolean,
        override val isRedesignEnabled: Boolean,
        val title: TextReference,
        val availableBalance: TextReference,
        val availableBalanceShort: TextReference,
        val tokenName: TextReference,
        val tokenIconState: CurrencyIconState,
        val segmentedButtonConfig: PersistentList<AmountSegmentedButtonsConfig>,
        val selectedButton: Int,
        val isSegmentedButtonsEnabled: Boolean,
        val amountTextField: AmountFieldModel,
        val appCurrency: AppCurrency,
        val isEditingDisabled: Boolean = false,
        val reduceAmountBy: BigDecimal = BigDecimal.ZERO,
        val isIgnoreReduce: Boolean = false,
    ) : AmountState()

    data class Empty(
        override val isPrimaryButtonEnabled: Boolean = false,
        override val isRedesignEnabled: Boolean,
    ) : AmountState()
}