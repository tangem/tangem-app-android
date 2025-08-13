package com.tangem.features.send.v2.api.params

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeSelectorUM

sealed class FeeSelectorParams {
    abstract val state: FeeSelectorUM
    abstract val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>
    abstract val cryptoCurrencyStatus: CryptoCurrencyStatus
    abstract val feeCryptoCurrencyStatus: CryptoCurrencyStatus
    abstract val feeStateConfiguration: FeeStateConfiguration
    abstract val feeDisplaySource: FeeDisplaySource
    abstract val analyticsCategoryName: String

    data class FeeSelectorBlockParams(
        override val state: FeeSelectorUM,
        override val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeStateConfiguration: FeeStateConfiguration,
        override val feeDisplaySource: FeeDisplaySource,
        override val analyticsCategoryName: String,
    ) : FeeSelectorParams()

    data class FeeSelectorDetailsParams(
        override val state: FeeSelectorUM,
        override val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeStateConfiguration: FeeStateConfiguration,
        override val feeDisplaySource: FeeDisplaySource,
        override val analyticsCategoryName: String,
        val callback: FeeSelectorModelCallback,
    ) : FeeSelectorParams()

    sealed class FeeStateConfiguration {
        data object None : FeeStateConfiguration()
        data class Suggestion(val title: TextReference, val fee: Fee) : FeeStateConfiguration()

        data object ExcludeLow : FeeStateConfiguration()
    }

    enum class FeeDisplaySource {
        Screen,
        BottomSheet,
    }
}