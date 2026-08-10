package com.tangem.feature.swap.domain.account

import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * Currencies a special account holds "under the hood". Today the Payment account returns exactly one
 * (USDC Polygon). Single seam to extend for multichain/Polymarket later without touching SwapModel.
 */
interface AccountUnderlyingCurrencies {
    suspend fun get(userWalletId: UserWalletId): List<CryptoCurrencyStatus>
}

internal class PaymentAccountUnderlyingCurrencies @Inject constructor(
    private val supplier: SingleAccountStatusListSupplier,
) : AccountUnderlyingCurrencies {

    override suspend fun get(userWalletId: UserWalletId): List<CryptoCurrencyStatus> {
        val payment = supplier.getSyncOrNull(SingleAccountStatusListProducer.Params(userWalletId))
            ?.accountStatuses.orEmpty()
            .filterIsInstance<AccountStatus.Payment>()
            .firstOrNull() ?: return emptyList()

        val status = when (val value = payment.value) {
            is PaymentAccountStatusValue.Loaded -> value.cryptoCurrencyStatus
            is PaymentAccountStatusValue.Deactivated -> value.cryptoCurrencyStatus
            else -> null
        }
        return listOfNotNull(status)
    }
}