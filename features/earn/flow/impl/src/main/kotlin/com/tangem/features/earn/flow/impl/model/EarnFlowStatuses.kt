package com.tangem.features.earn.flow.impl.model

import arrow.core.getOrElse
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/** The statuses the flow screens read: the product's currency, the currency that pays the fee and the app currency. */
internal class EarnFlowStatuses @Inject constructor(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
) {

    fun currency(product: EarnProduct.Params): Flow<CryptoCurrencyStatus> =
        singleAccountStatusListSupplier(product.userWalletId)
            .map { it.getCryptoCurrencyStatus(currency = product.currency).getOrNull() }
            .distinctUntilChanged()
            .onEach { status -> if (status == null) TangemLogger.e("Earn flow: no status for ${product.currency.id}") }
            .filterNotNull()

    fun appCurrency(): Flow<AppCurrency> = getSelectedAppCurrencyUseCase().mapNotNull { it.getOrNull() }

    /** For a token the fee is paid in the network's coin; falls back to [status] when that status is not found. */
    suspend fun feeCurrency(product: EarnProduct.Params, status: CryptoCurrencyStatus): CryptoCurrencyStatus {
        val feeStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(product.userWalletId, status).getOrNull()
        if (feeStatus == null) TangemLogger.e("Earn flow: no fee currency status for ${status.currency.id}")
        return feeStatus ?: status
    }

    /** Whether the fee may come out of the entered amount, as Send and Staking decide it. */
    suspend fun isAmountSubtractAvailable(product: EarnProduct.Params, selection: EarnFeeSelection): Boolean =
        isAmountSubtractAvailableUseCase(
            userWalletId = product.userWalletId,
            currency = product.currency,
            maybeGaslessFee = selection.extended
                ?.takeIf { it.gaslessProtocolData != null }
                ?.let { it.feeTokenId to it.transactionFee.normal },
        ).getOrElse { false }
}