package com.tangem.features.yield.supply.impl.common

import arrow.core.right
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.utils.extensions.orZero

/**
 * What the fee token list needs from a Yield Mode fee: the calls priced in the native coin, and the currency the fee
 * is shown in — which may be a picked token that could not pay, so the list keeps it selected.
 */
internal data class YieldSupplyFeeCurrencyChoice(
    val calls: List<TransactionData.Uncompiled>,
    val feeCurrencyId: CryptoCurrency.ID,
    val gaslessFee: TransactionFeeExtended?,
    val enteringYieldTokenId: CryptoCurrency.ID? = null,
) {

    /**
     * The list only offers currencies to pick: the host quotes the fee in the picked one itself, so the quote handed
     * to the list is the one already shown.
     */
    fun toFeeSelectorParams(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        nativeFeeCurrencyStatus: CryptoCurrencyStatus,
        callback: FeeSelectorModelCallback,
    ): FeeSelectorParams.FeeSelectorDetailsParams {
        val nativeFee = TransactionFee.Single(normal = summedNativeFee())
        val shownFee = TransactionFeeExtended(
            transactionFee = gaslessFee?.transactionFee ?: nativeFee,
            feeTokenId = feeCurrencyId,
            nativeFee = nativeFee,
        )
        return FeeSelectorParams.FeeSelectorDetailsParams(
            state = FeeSelectorUM.Loading,
            userWalletId = userWalletId,
            onLoadFeeExtended = { shownFee.right() },
            onLoadFee = { nativeFee.right() },
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            feeCryptoCurrencyStatus = nativeFeeCurrencyStatus,
            feeStateConfiguration = FeeSelectorParams.FeeStateConfiguration.None,
            feeDisplaySource = FeeSelectorParams.FeeDisplaySource.Screen,
            analyticsCategoryName = YieldSupplyAnalytics.CATEGORY,
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Earning,
            callback = callback,
            shouldShowOnlyTokenOption = true,
            enteringYieldTokenId = enteringYieldTokenId,
        )
    }

    private fun summedNativeFee(): Fee {
        val fees = calls.mapNotNull { it.fee }
        val amount = requireNotNull(fees.firstOrNull()) { "Yield Mode calls reach the fee list priced" }.amount
        return Fee.Common(amount = amount.copy(value = fees.sumOf { it.amount.value.orZero() }))
    }
}

/**
 * As on iOS, a picked token that can't cover the fee gets the "not enough" banner and any other failure offers a
 * reload — both block the button, whereas the raw gasless errors would show nothing and leave it enabled.
 */
internal fun GetFeeError.toPickedFeeCurrencyError(): GetFeeError = when (this) {
    GetFeeError.GaslessError.NotEnoughFunds,
    GetFeeError.GaslessError.NoSupportedTokensFound,
    -> GetFeeError.GaslessError.NotEnoughFunds
    else -> GetFeeError.UnknownError
}