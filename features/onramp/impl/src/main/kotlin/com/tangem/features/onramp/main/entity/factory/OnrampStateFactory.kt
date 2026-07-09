package com.tangem.features.onramp.main.entity.factory

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.message.TangemMessageIconPosition
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.tokens.model.convertToAmount
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.*
import com.tangem.utils.Provider
import java.math.BigDecimal
import com.tangem.core.ui.R as CoreUiR

internal class OnrampStateFactory(
    private val currentStateProvider: Provider<OnrampMainComponentUM>,
    private val onrampAmountButtonUMStateFactory: OnrampAmountButtonUMStateFactory,
    private val cryptoCurrency: CryptoCurrency,
    private val onrampIntents: OnrampIntents,
) {

    fun getInitialState(
        currency: String,
        onClose: () -> Unit,
        openSettings: () -> Unit,
    ): OnrampMainComponentUM.InitialLoading {
        return OnrampMainComponentUM.InitialLoading(
            errorNotification = null,
            topBarConfig = OnrampMainTopBarUM(
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

    fun getReadyState(currency: OnrampCurrency, initialFiatAmount: BigDecimal? = null): OnrampMainComponentUM.Content {
        val state = currentStateProvider()

        val endButton = when (val button = state.topBarConfig.endButtonUM) {
            is TopAppBarButtonUM.Icon -> button.copy(isEnabled = true)
            is TopAppBarButtonUM.Text -> button.copy(isEnabled = true)
        }

        val initialAmountBlockState = getInitialAmountBlockState(currency, initialFiatAmount)

        return OnrampMainComponentUM.Content(
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
            OnrampError.AlreadyHandledTransaction,
            -> currentStateProvider() // ignore error state
        }
    }

    fun getErrorState(errorCode: String? = null, onRefresh: () -> Unit): OnrampMainComponentUM {
        val state = currentStateProvider()
        val endButton = when (val button = state.topBarConfig.endButtonUM) {
            is TopAppBarButtonUM.Icon -> button.copy(isEnabled = true)
            is TopAppBarButtonUM.Text -> button.copy(isEnabled = true)
        }

        return when (state) {
            is OnrampMainComponentUM.Content -> state.copy(
                topBarConfig = state.topBarConfig.copy(endButtonUM = endButton),
                offersBlockState = OnrampOffersBlockUM.Empty,
                errorNotification = NotificationUM.Warning.OnrampErrorNotification(
                    errorCode = errorCode,
                    onRefresh = onRefresh,
                ),
                onrampAmountButtonUMState = OnrampAmountButtonUMState.None,
                amountBlockState = state.amountBlockState.copy(
                    secondaryFieldModel = OnrampSecondaryFieldErrorUM.Empty,
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

    fun getBuyNotSupportedState(state: OnrampMainComponentUM = currentStateProvider()): OnrampMainComponentUM {
        val message = buildBuyNotSupportedMessage()

        return when (state) {
            is OnrampMainComponentUM.Content -> state.copy(
                buyNotSupportedMessage = message,
                errorNotification = null,
                offersBlockState = OnrampOffersBlockUM.Empty,
                onrampAmountButtonUMState = OnrampAmountButtonUMState.None,
                amountBlockState = state.amountBlockState.copy(
                    amountFieldModel = state.amountBlockState.amountFieldModel.copy(isError = true),
                    secondaryFieldModel = OnrampSecondaryFieldErrorUM.Empty,
                ),
            )
            is OnrampMainComponentUM.InitialLoading -> state.copy(
                buyNotSupportedMessage = message,
                errorNotification = null,
            )
        }
    }

    private fun buildBuyNotSupportedMessage(): TangemMessageUM = TangemMessageUM(
        id = "buy_not_supported",
        title = resourceReference(
            id = R.string.onramp_token_is_not_supported_banner_title,
            formatArgs = wrappedList(cryptoCurrency.name),
        ),
        subtitle = resourceReference(R.string.onramp_token_is_not_supported_banner_subtitle),
        messageEffect = TangemMessageEffect.None,
        iconUM = TangemIconUM.Icon(
            iconRes = CoreUiR.drawable.ic_attention_default_24,
            tintReference = { TangemTheme.colors2.graphic.status.attention },
        ),
        iconPosition = TangemMessageIconPosition.Leading,
    )

    private fun getNoPairsErrorState(): OnrampMainComponentUM {
        val state = currentStateProvider()
        val contentState = state as? OnrampMainComponentUM.Content ?: return state

        return contentState.copy(
            amountBlockState = contentState.amountBlockState.copy(
                amountFieldModel = contentState.amountBlockState.amountFieldModel.copy(isError = true),
                secondaryFieldModel = OnrampSecondaryFieldErrorUM.Error(
                    error = resourceReference(R.string.onramp_no_available_providers),
                ),
            ),
            onrampAmountButtonUMState = OnrampAmountButtonUMState.None,
            offersBlockState = OnrampOffersBlockUM.Empty,
        )
    }

    private fun getInitialAmountBlockState(
        currency: OnrampCurrency,
        initialFiatAmount: BigDecimal? = null,
    ): OnrampAmountBlockUM {
        return OnrampAmountBlockUM(
            currencyUM = OnrampCurrencyUM(
                code = currency.code,
                iconUrl = currency.image,
                precision = currency.precision,
                onClick = onrampIntents::openCurrenciesList,
                unit = currency.unit,
            ),
            amountFieldModel = AmountFieldModel(
                value = initialFiatAmount?.toPlainString().orEmpty(),
                fiatValue = initialFiatAmount?.toPlainString().orEmpty(),
                onValueChange = onrampIntents::onAmountValueChanged,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.None,
                    keyboardType = KeyboardType.Number,
                ),
                keyboardActions = KeyboardActions(),
                isFiatValue = true,
                cryptoAmount = BigDecimal.ZERO.convertToAmount(cryptoCurrency),
                fiatAmount = (initialFiatAmount ?: BigDecimal.ZERO).convertToFiatAmount(currency),
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