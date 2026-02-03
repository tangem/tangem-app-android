package com.tangem.features.send.v2.api.params

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeSelectorUM

sealed class FeeSelectorParams {
    abstract val state: FeeSelectorUM
    abstract val userWalletId: UserWalletId
    abstract val onLoadFeeExtended: (suspend (CryptoCurrencyStatus?) -> Either<GetFeeError, TransactionFeeExtended>)?
    abstract val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>
    abstract val cryptoCurrencyStatus: CryptoCurrencyStatus
    abstract val feeCryptoCurrencyStatus: CryptoCurrencyStatus
    abstract val feeStateConfiguration: FeeStateConfiguration
    abstract val feeDisplaySource: FeeDisplaySource
    abstract val analyticsCategoryName: String
    abstract val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource
    abstract val shouldShowOnlySpeedOption: Boolean

    data class FeeSelectorBlockParams(
        override val state: FeeSelectorUM,
        override val userWalletId: UserWalletId,
        override val onLoadFeeExtended: (
            suspend (CryptoCurrencyStatus?) -> Either<GetFeeError, TransactionFeeExtended>
        )? = null,
        override val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeStateConfiguration: FeeStateConfiguration,
        override val feeDisplaySource: FeeDisplaySource,
        override val analyticsCategoryName: String,
        override val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
        override val shouldShowOnlySpeedOption: Boolean = false,
        val bottomSheetShown: (Boolean) -> Unit = {},
    ) : FeeSelectorParams()

    data class FeeSelectorDetailsParams(
        override val state: FeeSelectorUM,
        override val userWalletId: UserWalletId,
        override val onLoadFeeExtended: (
            suspend (CryptoCurrencyStatus?) -> Either<GetFeeError, TransactionFeeExtended>
        )? = null,
        override val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeStateConfiguration: FeeStateConfiguration,
        override val feeDisplaySource: FeeDisplaySource,
        override val analyticsCategoryName: String,
        override val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
        override val shouldShowOnlySpeedOption: Boolean = false,
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