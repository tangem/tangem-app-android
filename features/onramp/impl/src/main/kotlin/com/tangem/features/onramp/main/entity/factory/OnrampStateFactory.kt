package com.tangem.features.onramp.main.entity.factory

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.convertToAmount
import com.tangem.features.onramp.main.entity.*
import com.tangem.features.onramp.main.entity.OnrampAmountBlockUM
import com.tangem.features.onramp.main.entity.OnrampAmountSecondaryFieldUM
import com.tangem.features.onramp.main.entity.OnrampCurrencyUM
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.utils.Provider
import java.math.BigDecimal

internal class OnrampStateFactory(
    private val currentStateProvider: Provider<OnrampMainComponentUM>,
    private val cryptoCurrency: CryptoCurrency,
    private val onrampIntents: OnrampIntents,
) {

    fun getInitialState(currency: String, onClose: () -> Unit): OnrampMainComponentUM.InitialLoading {
        return OnrampMainComponentUM.InitialLoading(
            currency = currency,
            onClose = onClose,
            openSettings = onrampIntents::openSettings,
            onBuyClick = onrampIntents::onBuyClick,
        )
    }

    fun getReadyState(currency: OnrampCurrency): OnrampMainComponentUM.Content {
        val state = currentStateProvider()

        val endButton = state.topBarConfig.endButtonUM.copy(enabled = true)
        return OnrampMainComponentUM.Content(
            topBarConfig = state.topBarConfig.copy(endButtonUM = endButton),
            buyButtonConfig = state.buyButtonConfig,
            amountBlockState = getInitialAmountBlockState(currency),
        )
    }

    private fun getInitialAmountBlockState(currency: OnrampCurrency): OnrampAmountBlockUM {
        return OnrampAmountBlockUM(
            currencyUM = OnrampCurrencyUM(
                code = currency.code,
                iconUrl = currency.image,
                precision = currency.precision,
                onClick = onrampIntents::openCurrenciesList,
            ),
            amountFieldModel = AmountFieldModel(
                value = "",
                fiatValue = "",
                onValueChange = onrampIntents::onAmountValueChanged,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.None,
                    keyboardType = KeyboardType.Number,
                ),
                keyboardActions = KeyboardActions(),
                isFiatValue = true,
                cryptoAmount = BigDecimal.ZERO.convertToAmount(cryptoCurrency),
                fiatAmount = BigDecimal.ZERO.convertToFiatAmount(currency),
                isError = false,
                isWarning = false,
                error = TextReference.EMPTY,
                isFiatUnavailable = false,
                isValuePasted = false,
                onValuePastedTriggerDismiss = {},
            ),
            secondaryFieldModel = OnrampAmountSecondaryFieldUM.Loading,
        )
    }

    private fun BigDecimal.convertToFiatAmount(currency: OnrampCurrency): Amount = Amount(
        currencySymbol = currency.name,
        value = this,
        decimals = currency.precision,
        type = AmountType.FiatType(currency.code),
    )
}