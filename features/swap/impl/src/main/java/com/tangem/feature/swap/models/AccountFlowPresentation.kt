package com.tangem.feature.swap.models

import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.feature.swap.presentation.R

/** Abstract currency label for the TO card in an account top-up flow. */
private const val ACCOUNT_TOP_UP_ABSTRACT_SYMBOL = "USD"

/**
 * Applied to every state the model emits rather than inside the builders that produce them, so a state
 * assembled anywhere still gets the same treatment. The main button is deliberately left alone: it keeps
 * its dynamic Transfer/Swap label in account flows too.
 */
internal fun SwapStateHolder.withAccountFlowPresentation(
    accountFlow: AccountFlow?,
    isReverseLocked: Boolean,
): SwapStateHolder {
    val reversedState = if (isReverseLocked) {
        copy(changeCardsButtonState = ChangeCardsButtonState.HIDDEN)
    } else {
        this
    }

    val flow = accountFlow ?: return reversedState

    val titledState = reversedState.copy(titleId = flow.titleId())

    return when (flow) {
        is AccountFlow.PredictionWithdraw -> titledState.withPredictionWithdrawCards()
        is AccountFlow.PredictionTopUp -> titledState.withLockedReceiveCard()
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

private fun SwapStateHolder.withPredictionWithdrawCards(): SwapStateHolder = copy(
    swapButton = swapButton.asWithdrawButton(),
    sendCardData = sendCardData.withSelectionLocked(),
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