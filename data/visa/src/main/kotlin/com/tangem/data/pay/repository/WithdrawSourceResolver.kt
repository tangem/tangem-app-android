package com.tangem.data.pay.repository

import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Resolves the payment-account network the funds are drawn from, in the shape the withdrawal endpoints
 * expect: `chain_id` comes from the backend rather than from the blockchain SDK, since non-EVM payment
 * networks (Tron) have no chain id there.
 */
internal class WithdrawSourceResolver @Inject constructor(
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) {

    /**
     * `null` when the account is issued on networks but none of them is [sourceCurrency]'s: withdrawing then
     * would move funds from whichever network the backend defaults to, while the amount was computed from the
     * token the user picked. Callers must abort instead.
     *
     * Both values are `null` for an account with no issued networks — the pre-multichain case, where the
     * backend's default network is the account's only one.
     */
    suspend fun resolve(userWalletId: UserWalletId, sourceCurrency: CryptoCurrency): WithdrawSource? {
        val accountStatus = paymentAccountStatusSupplier.invoke(userWalletId).firstOrNull()
        val networks = when (val statusValue = accountStatus?.value) {
            is PaymentAccountStatusValue.Loaded -> statusValue.networks
            is PaymentAccountStatusValue.Deactivated -> statusValue.networks
            else -> emptyList()
        }
        val issuedNetworks = networks.filterIsInstance<PaymentNetworkStatus.Available>()
        if (issuedNetworks.isEmpty()) return WithdrawSource(chainId = null, tokenContractAddress = null)

        val network = issuedNetworks.firstOrNull { it.network.id == sourceCurrency.network.id } ?: return null

        return WithdrawSource(
            chainId = network.chainId,
            tokenContractAddress = (sourceCurrency as? CryptoCurrency.Token)?.contractAddress,
        )
    }

    data class WithdrawSource(val chainId: Long?, val tokenContractAddress: String?)
}