package com.tangem.feature.swap.models

import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.feature.swap.presentation.R
import java.math.BigDecimal
import java.math.RoundingMode

/** Abstract currency label for the TO card in an account top-up flow. */
private const val ACCOUNT_TOP_UP_ABSTRACT_SYMBOL = "USD"
private const val PREDICTION_BALANCE_DECIMALS = 6
private const val USD_BALANCE_MIN_DECIMALS = 2

/**
 * Applied to every state the model emits rather than inside the builders that produce them, so a state
 * assembled anywhere still gets the same treatment. The main button is deliberately left alone: it keeps
 * its dynamic Transfer/Swap label in account flows too.
 *
 * @param predictionCollateralBalance the prediction account balance for top-up or withdrawal
 */
internal fun SwapStateHolder.withAccountFlowPresentation(
    accountFlow: AccountFlow?,
    isReverseLocked: Boolean,
    predictionCollateralBalance: BigDecimal? = null,
): SwapStateHolder {
    val reversedState = if (isReverseLocked) {
        copy(changeCardsButtonState = ChangeCardsButtonState.HIDDEN)
    } else {
        this
    }

    val flow = accountFlow ?: return reversedState

    val titledState = reversedState.copy(titleId = flow.titleId())

    return when (flow) {
        is AccountFlow.PredictionWithdraw -> titledState.withPredictionWithdrawCards(predictionCollateralBalance)
        is AccountFlow.PredictionTopUp -> titledState.withPredictionTopUpReceiveCard(predictionCollateralBalance)
        is AccountFlow.CollectiblesTopUp -> titledState.withLockedReceiveCard()
        is AccountFlow.TopUp -> titledState.withAccountTopUpReceiveCard()
        is AccountFlow.Withdraw -> titledState
    }
}

private fun AccountFlow.titleId(): Int = when (this) {
    is AccountFlow.TopUp -> R.string.tangempay_card_details_add_funds
    is AccountFlow.Withdraw -> R.string.tangempay_card_details_withdraw
    is AccountFlow.PredictionWithdraw -> R.string.prediction_main_withdraw
    is AccountFlow.PredictionTopUp -> R.string.common_add_funds
    is AccountFlow.CollectiblesTopUp -> R.string.common_add_funds
}

private fun SwapStateHolder.withPredictionWithdrawCards(collateralBalance: BigDecimal?): SwapStateHolder = copy(
    swapButton = swapButton.asWithdrawButton(),
    sendCardData = when (val sendCard = sendCardData) {
        is SwapCardState.SwapCardData -> sendCard.copy(
            currencyIconState = CurrencyIconState.PredictionAccount(),
            fiatSymbolOverride = AppCurrency.Default.code,
            amountCurrencyOverride = AppCurrency.Default,
            balance = collateralBalance.formatWithdrawUsdBalance(),
            isSelectionLocked = true,
        )
        else -> sendCard.withSelectionLocked()
    },
)

private fun SwapStateHolder.withLockedReceiveCard(): SwapStateHolder = copy(
    receiveCardData = receiveCardData.withSelectionLocked(),
)

private fun SwapStateHolder.withAccountTopUpReceiveCard(): SwapStateHolder {
    val receiveCard = receiveCardData as? SwapCardState.SwapCardData ?: return withLockedReceiveCard()
    return copy(
        receiveCardData = receiveCard.copy(
            currencyIconState = CurrencyIconState.PaymentAccount(),
            fiatSymbolOverride = ACCOUNT_TOP_UP_ABSTRACT_SYMBOL,
            isSelectionLocked = true,
        ),
    )
}

private fun SwapStateHolder.withPredictionTopUpReceiveCard(collateralBalance: BigDecimal?): SwapStateHolder {
    val receiveCard = receiveCardData as? SwapCardState.SwapCardData ?: return withLockedReceiveCard()
    return copy(
        receiveCardData = receiveCard.copy(
            currencyIconState = CurrencyIconState.PredictionAccount(),
            fiatSymbolOverride = ACCOUNT_TOP_UP_ABSTRACT_SYMBOL,
            balance = collateralBalance?.formatUsdBalance() ?: receiveCard.balance,
            isSelectionLocked = true,
        ),
    )
}

private fun BigDecimal?.formatWithdrawUsdBalance(): TextReference {
    val amount = this?.setScale(PREDICTION_BALANCE_DECIMALS, RoundingMode.DOWN)
    val decimals = amount?.stripTrailingZeros()?.scale()?.coerceAtLeast(USD_BALANCE_MIN_DECIMALS)
        ?: USD_BALANCE_MIN_DECIMALS
    return amount.formatUsdBalance(decimals)
}

private fun BigDecimal?.formatUsdBalance(decimals: Int? = null): TextReference = resourceReference(
    id = R.string.common_balance,
    formatArgs = wrappedList(
        format {
            val formatter = fiat(
                fiatCurrencyCode = AppCurrency.Default.code,
                fiatCurrencySymbol = AppCurrency.Default.symbol,
            )
            if (decimals == null) formatter else formatter.anyDecimals(decimals)
        },
    ),
)

private fun SwapCardState.withSelectionLocked(): SwapCardState = when (this) {
    is SwapCardState.SwapCardData -> copy(isSelectionLocked = true)
    is SwapCardState.Empty -> copy(isSelectionLocked = true)
    is SwapCardState.Loading -> this
}

private fun SwapButton.asWithdrawButton(): SwapButton = if (isInProgress) {
    copy(mode = SwapButton.Mode.TRANSFER_PROGRESSING, title = resourceReference(R.string.common_withdrawing))
} else {
    copy(mode = SwapButton.Mode.TRANSFER, title = resourceReference(R.string.prediction_main_withdraw))
}