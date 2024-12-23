package com.tangem.features.onramp.main.entity.factory

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.convertToAmount
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.*
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
        )
    }

    fun getReadyState(currency: OnrampCurrency): OnrampMainComponentUM.Content {
        val state = currentStateProvider()

        val endButton = state.topBarConfig.endButtonUM.copy(enabled = true)
        return OnrampMainComponentUM.Content(
            topBarConfig = state.topBarConfig.copy(endButtonUM = endButton),
            buyButtonConfig = state.buyButtonConfig,
            amountBlockState = getInitialAmountBlockState(currency),
            providerBlockState = OnrampProviderBlockUM.Empty,
            errorNotification = null,
        )
    }

    fun getOnrampErrorState(onrampError: OnrampError): OnrampMainComponentUM {
        return when (onrampError) {
            OnrampError.PairsNotFound -> getNoPairsErrorState()
            is OnrampError.DataError -> getErrorState(
                errorCode = onrampError.code,
                onRefresh = onrampIntents::onRefresh,
            )
            is OnrampError.DomainError -> getErrorState(onRefresh = onrampIntents::onRefresh)
            is OnrampError.AmountError.TooBigError,
            is OnrampError.AmountError.TooSmallError,
            OnrampError.RedirectError.VerificationFailed,
            OnrampError.RedirectError.WrongRequestId,
            -> currentStateProvider() // ignore error state
        }
    }

    private fun getNoPairsErrorState(): OnrampMainComponentUM {
        val state = currentStateProvider()
        val contentState = state as? OnrampMainComponentUM.Content ?: return state

        return contentState.copy(
            buyButtonConfig = contentState.buyButtonConfig.copy(enabled = false),
            amountBlockState = contentState.amountBlockState.copy(
                amountFieldModel = contentState.amountBlockState.amountFieldModel.copy(isError = true),
                secondaryFieldModel = OnrampAmountSecondaryFieldUM.Error(
                    error = resourceReference(R.string.onramp_no_available_providers),
                ),
            ),
        )
    }

    fun getErrorState(errorCode: String? = null, onRefresh: () -> Unit): OnrampMainComponentUM {
        val state = currentStateProvider()
        val endButton = state.topBarConfig.endButtonUM.copy(enabled = true)

        return when (state) {
            is OnrampMainComponentUM.Content -> state.copy(
                topBarConfig = state.topBarConfig.copy(endButtonUM = endButton),
                buyButtonConfig = state.buyButtonConfig.copy(enabled = false),
                amountBlockState = state.amountBlockState.copy(
                    amountFieldModel = state.amountBlockState.amountFieldModel.copy(isError = true),
                    secondaryFieldModel = OnrampAmountSecondaryFieldUM.Content(TextReference.EMPTY),
                ),
                providerBlockState = OnrampProviderBlockUM.Empty,
                errorNotification = NotificationUM.Warning.OnrampErrorNotification(
                    errorCode = errorCode,
                    onRefresh = onRefresh,
                ),
            )
            is OnrampMainComponentUM.InitialLoading -> state.copy(
                errorNotification = NotificationUM.Warning.OnrampErrorNotification(
                    errorCode = errorCode,
                    onRefresh = onRefresh,
                ),
            )
        }
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
            secondaryFieldModel = OnrampAmountSecondaryFieldUM.Content(TextReference.EMPTY),
        )
    }

    private fun BigDecimal.convertToFiatAmount(currency: OnrampCurrency): Amount = Amount(
        currencySymbol = currency.unit,
        value = this,
        decimals = currency.precision,
        type = AmountType.FiatType(currency.code),
    )
}