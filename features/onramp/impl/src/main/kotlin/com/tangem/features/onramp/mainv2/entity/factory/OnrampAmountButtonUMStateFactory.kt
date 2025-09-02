package com.tangem.features.onramp.mainv2.entity.factory

import com.tangem.features.onramp.mainv2.entity.OnrampAmountButtonUM
import com.tangem.features.onramp.mainv2.entity.OnrampV2AmountButtonUMState
import kotlinx.collections.immutable.toPersistentList

internal class OnrampAmountButtonUMStateFactory {

    private val defaultPreselectedAmount = listOf(50, 100, 200, 300, 500)

    fun createOnrampAmountActionButton(
        currencyCode: String,
        currencySymbol: String,
        onAmountValueChanged: (String) -> Unit,
    ): OnrampV2AmountButtonUMState {
        return when (currencyCode) {
            USD_CODE, EUR_CODE -> {
                val buttons = defaultPreselectedAmount.map { value ->
                    OnrampAmountButtonUM(
                        value = value,
                        currency = currencySymbol,
                        onClick = { onAmountValueChanged(value.toString()) },
                    )
                }.toPersistentList()
                OnrampV2AmountButtonUMState.Loaded(buttons)
            }
            else -> OnrampV2AmountButtonUMState.None
        }
    }

    companion object {
        private const val USD_CODE = "USD"
        private const val EUR_CODE = "EUR"
    }
}