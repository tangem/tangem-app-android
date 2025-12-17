package com.tangem.features.onramp.mainv2.entity.factory

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.tokens.model.convertToAmount
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.mainv2.entity.*
import com.tangem.utils.Provider
import java.math.BigDecimal

internal class OnrampV2StateFactory(
    private val currentStateProvider: Provider<OnrampV2MainComponentUM>,
    private val onrampAmountButtonUMStateFactory: OnrampAmountButtonUMStateFactory,
    private val cryptoCurrency: CryptoCurrency,
    private val onrampIntents: OnrampV2Intents,
) {

    fun getInitialState(
        currency: String,
        onClose: () -> Unit,
        openSettings: () -> Unit,
    ): OnrampV2MainComponentUM.InitialLoading {
        return OnrampV2MainComponentUM.InitialLoading(
            errorNotification = null,
            topBarConfig = OnrampV2MainTopBarUM(
                title = combinedReference(resourceReference(R.string.common_buy), stringReference(" $currency")),
                startButtonUM = TopAppBarButtonUM.Close(
                    onCloseClick = onClose,
                    enabled = true,
                ),
                endButtonUM = TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_more_vertical_24,
                    onClicked = openSettings,
                    isEnabled = false,
                ),
            ),
        )
    }

    fun getReadyState(currency: OnrampCurrency): OnrampV2MainComponentUM.Content {
        val state = currentStateProvider()

        val endButton = when (val button = state.topBarConfig.endButtonUM) {
            is TopAppBarButtonUM.Icon -> button.copy(isEnabled = true)
            is TopAppBarButtonUM.Text -> button.copy(isEnabled = true)
        }

        val initialAmountBlockState = getInitialAmountBlockState(currency)

        return OnrampV2MainComponentUM.Content(
            topBarConfig = state.topBarConfig.copy(endButtonUM = endButton),
            amountBlockState = initialAmountBlockState,
            offersBlockState = OnrampOffersBlockUM.Empty,
            errorNotification = null,
            onrampAmountButtonUMState = onrampAmountButtonUMStateFactory.createOnrampAmountActionButton(
                currencyCode = currency.code,
                currencySymbol = currency.unit,
                onAmountValueChanged = onrampIntents::onAmountValueChanged,
            ),
        )
    }

    fun getOnrampErrorState(onrampError: OnrampError): OnrampV2MainComponentUM {
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
            OnrampError.AlreadyHandledTransaction,
            -> currentStateProvider() // ignore error state
        }
    }

    fun getErrorState(errorCode: String? = null, onRefresh: () -> Unit): OnrampV2MainComponentUM {
        val state = currentStateProvider()
        val endButton = when (val button = state.topBarConfig.endButtonUM) {
            is TopAppBarButtonUM.Icon -> button.copy(isEnabled = true)
            is TopAppBarButtonUM.Text -> button.copy(isEnabled = true)
        }

        return when (state) {
            is OnrampV2MainComponentUM.Content -> state.copy(
                topBarConfig = state.topBarConfig.copy(endButtonUM = endButton),
                offersBlockState = OnrampOffersBlockUM.Empty,
                errorNotification = NotificationUM.Warning.OnrampErrorNotification(
                    errorCode = errorCode,
                    onRefresh = onRefresh,
                ),
                onrampAmountButtonUMState = OnrampV2AmountButtonUMState.None,
                amountBlockState = state.amountBlockState.copy(
                    secondaryFieldModel = OnrampSecondaryFieldErrorUM.Empty,
                ),
            )
            is OnrampV2MainComponentUM.InitialLoading -> state.copy(
                errorNotification = NotificationUM.Warning.OnrampErrorNotification(
                    errorCode = errorCode,
                    onRefresh = onRefresh,
                ),
            )
        }
    }

    private fun getNoPairsErrorState(): OnrampV2MainComponentUM {
        val state = currentStateProvider()
        val contentState = state as? OnrampV2MainComponentUM.Content ?: return state

        return contentState.copy(
            amountBlockState = contentState.amountBlockState.copy(
                amountFieldModel = contentState.amountBlockState.amountFieldModel.copy(isError = true),
                secondaryFieldModel = OnrampSecondaryFieldErrorUM.Error(
                    error = resourceReference(R.string.onramp_no_available_providers),
                ),
            ),
            onrampAmountButtonUMState = OnrampV2AmountButtonUMState.None,
            offersBlockState = OnrampOffersBlockUM.Empty,
        )
    }

    private fun getInitialAmountBlockState(currency: OnrampCurrency): OnrampNewAmountBlockUM {
        return OnrampNewAmountBlockUM(
            currencyUM = OnrampNewCurrencyUM(
                code = currency.code,
                iconUrl = currency.image,
                precision = currency.precision,
                onClick = onrampIntents::openCurrenciesList,
                unit = currency.unit,
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
            secondaryFieldModel = OnrampSecondaryFieldErrorUM.Empty,
        )
    }

    private fun BigDecimal.convertToFiatAmount(currency: OnrampCurrency): Amount = Amount(
        currencySymbol = currency.unit,
        value = this,
        decimals = currency.precision,
        type = AmountType.FiatType(currency.code),
    )
}