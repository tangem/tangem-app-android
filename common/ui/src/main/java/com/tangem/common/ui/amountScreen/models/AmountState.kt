package com.tangem.common.ui.amountScreen.models

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.PersistentList

/** Model for amount state */
@Stable
sealed class AmountState {

    abstract val isPrimaryButtonEnabled: Boolean

    /**
     * @param isPrimaryButtonEnabled indicates if next state button enabled
     * @param title title
     * @param availableBalance user crypto currency balance
     * @param tokenIconState crypto currency icon state
     * @param segmentedButtonConfig currency switcher config
     * @param selectedButton selected currency index
     * @param isSegmentedButtonsEnabled indicates if currency switches is enabled
     * @param amountTextField amount field state
     * @param appCurrencyCode app currency code
     */
    data class Data(
        override val isPrimaryButtonEnabled: Boolean,
        val title: TextReference,
        val availableBalance: TextReference,
        val tokenIconState: CurrencyIconState,
        val segmentedButtonConfig: PersistentList<AmountSegmentedButtonsConfig>,
        val selectedButton: Int,
        val isSegmentedButtonsEnabled: Boolean,
        val amountTextField: AmountFieldModel,
        val appCurrencyCode: String,
    ) : AmountState()

    data class Empty(
        override val isPrimaryButtonEnabled: Boolean = false,
    ) : AmountState()
}