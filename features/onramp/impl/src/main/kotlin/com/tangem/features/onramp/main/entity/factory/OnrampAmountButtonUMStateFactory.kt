package com.tangem.features.onramp.main.entity.factory

import com.tangem.features.onramp.main.entity.OnrampAmountButtonUM
import com.tangem.features.onramp.main.entity.OnrampAmountButtonUMState
import kotlinx.collections.immutable.toPersistentList

internal class OnrampAmountButtonUMStateFactory {

    private val defaultPreselectedAmount = listOf(50, 100, 200, 300, 500)

    fun createOnrampAmountActionButton(
        currencyCode: String,
        currencySymbol: String,
        onAmountValueChanged: (String) -> Unit,
    ): OnrampAmountButtonUMState {
        return when (currencyCode) {
            USD_CODE, EUR_CODE -> {
                val buttons = defaultPreselectedAmount.map { value ->
                    OnrampAmountButtonUM(
                        value = value,
                        currency = currencySymbol,
                        onClick = { onAmountValueChanged(value.toString()) },
                    )
                }.toPersistentList()
                OnrampAmountButtonUMState.Loaded(buttons)
            }
            else -> OnrampAmountButtonUMState.None
        }
    }

    companion object {
        private const val USD_CODE = "USD"
        private const val EUR_CODE = "EUR"
    }
}