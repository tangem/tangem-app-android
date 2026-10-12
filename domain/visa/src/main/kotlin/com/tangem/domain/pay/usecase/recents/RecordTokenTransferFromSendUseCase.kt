package com.tangem.domain.pay.usecase.recents

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.PaymentAccountRecentsStore
import kotlinx.coroutines.flow.firstOrNull

/**
 * Records a plain send whose recipient is one of the payment account's deposit addresses: nothing else marks
 * such a transaction as a top-up of the account, so it is recognised by the destination address and the network.
 */
class RecordTokenTransferFromSendUseCase(
    private val recentsStore: PaymentAccountRecentsStore,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        sourceAccount: Account?,
        currency: CryptoCurrency,
        destinationAddress: String,
    ) {
        val account = sourceAccount as? Account.CryptoPortfolio ?: return
        val accountStatus = paymentAccountStatusSupplier(userWalletId).firstOrNull() ?: return
        val statusValue = accountStatus.value as? PaymentAccountStatusValue.Loaded ?: return

        val isDeposit = statusValue.networks
            .filterIsInstance<PaymentNetworkStatus.Available>()
            .any { networkStatus ->
                networkStatus.network.id.rawId == currency.network.id.rawId &&
                    networkStatus.depositAddress.equals(destinationAddress, ignoreCase = true)
            }

        if (!isDeposit) return

        recentsStore.markUsed(userWalletId = userWalletId, accountId = account.accountId, currencyId = currency.id)
    }
}