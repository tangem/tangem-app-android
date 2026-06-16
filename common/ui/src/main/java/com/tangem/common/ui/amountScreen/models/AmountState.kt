package com.tangem.common.ui.amountScreen.models

import androidx.compose.runtime.Stable
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

/** Model for amount state */
@Stable
sealed class AmountState {

    abstract val isPrimaryButtonEnabled: Boolean

    /**
     * @param isPrimaryButtonEnabled indicates if next state button enabled
     * @param accountTitleUM info about current account or wallet
     * @param availableBalanceCrypto user crypto currency balance in crypto
     * @param availableBalanceFiat user crypto currency balance in fiat
     * @param tokenIconState crypto currency icon state
     * @param amountTextField amount field state
     * @param appCurrency app currency
     * @param isEditingDisabled indicated whether amount is editable
     * @param reduceAmountBy reduces amount to be sent by specified value
     * @param isIgnoreReduce ignores reduce amount value
     */
    data class Data(
        override val isPrimaryButtonEnabled: Boolean,
        val accountTitleUM: AccountTitleUM,
        val availableBalanceCrypto: TextReference,
        val availableBalanceFiat: TextReference,
        val tokenName: TextReference,
        val tokenIconState: CurrencyIconState,
        val amountTextField: AmountFieldModel,
        val appCurrency: AppCurrency,
        val isEditingDisabled: Boolean = false,
        val reduceAmountBy: BigDecimal = BigDecimal.ZERO,
        val isIgnoreReduce: Boolean = false,
    ) : AmountState()

    data object Empty : AmountState() {
        override val isPrimaryButtonEnabled: Boolean = false
    }
}