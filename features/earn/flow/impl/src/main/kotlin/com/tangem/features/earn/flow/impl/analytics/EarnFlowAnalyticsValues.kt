package com.tangem.features.earn.flow.impl.analytics

import com.tangem.features.earn.product.api.analytics.EarnScreenAnalytics
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.Withdraw

internal enum class EarnFlowScreen(val value: String) {
    Deposit("Deposit"),
    Withdraw("Withdraw"),
    Confirm("Confirm"),
}

internal enum class EarnTxType(val value: String) {
    Deposit("Deposit"),
    Withdraw("Withdraw"),
}

internal enum class EarnDisplayCurrency(val value: String) {
    Crypto("Crypto"),
    Fiat("Fiat"),
}

internal enum class EarnErrorPopupAction(val value: String) {
    Close("Close"),
    Support("Support"),
}

internal enum class EarnResultDestination(val value: String) {
    Token("Token"),
    Yield("Yield"),
}

internal fun EarnFlowAction.toTxType(): EarnTxType? = when (this) {
    is Deposit -> EarnTxType.Deposit
    is Withdraw -> EarnTxType.Withdraw
    else -> null
}

internal fun EarnTxType.amountScreen(): EarnFlowScreen = when (this) {
    EarnTxType.Deposit -> EarnFlowScreen.Deposit
    EarnTxType.Withdraw -> EarnFlowScreen.Withdraw
}

internal fun EarnFlowExit.toResultDestination(): EarnResultDestination? = when (this) {
    EarnFlowExit.ToToken -> EarnResultDestination.Token
    EarnFlowExit.ToYield -> EarnResultDestination.Yield
    EarnFlowExit.Done -> null
}

internal fun EarnScreenAnalytics.isLiquidityCapped(): Boolean? {
    if (!isLiquidityKnown) return null
    val position = position ?: return null
    val available = availableToWithdraw ?: return null
    return available < position
}