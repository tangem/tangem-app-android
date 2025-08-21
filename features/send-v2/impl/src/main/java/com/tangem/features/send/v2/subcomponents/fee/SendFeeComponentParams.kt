package com.tangem.features.send.v2.subcomponents.fee

import arrow.core.Either
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

internal sealed class SendFeeComponentParams {

    abstract val state: FeeUM
    abstract val analyticsCategoryName: String
    abstract val userWallet: UserWallet
    abstract val cryptoCurrencyStatus: CryptoCurrencyStatus
    abstract val feeCryptoCurrencyStatus: CryptoCurrencyStatus
    abstract val appCurrency: AppCurrency
    abstract val sendAmount: BigDecimal
    abstract val destinationAddress: String
    abstract val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>

    data class FeeParams(
        override val state: FeeUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        override val appCurrency: AppCurrency,
        override val sendAmount: BigDecimal,
        override val destinationAddress: String,
        override val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        val currentRoute: Flow<CommonSendRoute.Fee>,
        val callback: SendFeeComponent.ModelCallback,
    ) : SendFeeComponentParams()

    data class FeeBlockParams(
        override val state: FeeUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        override val appCurrency: AppCurrency,
        override val sendAmount: BigDecimal,
        override val destinationAddress: String,
        override val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        val blockClickEnableFlow: StateFlow<Boolean>,
    ) : SendFeeComponentParams()
}