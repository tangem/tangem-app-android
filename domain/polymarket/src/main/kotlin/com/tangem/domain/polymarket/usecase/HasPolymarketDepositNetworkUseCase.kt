package com.tangem.domain.polymarket.usecase

import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketDepositBlockchain

/**
 * Whether [userWalletId] already holds the deposit chain, which onboarding requires before it can run.
 *
 * The chain is the whole question — any currency on it will do, native or token. Which currency the user is
 * then offered to add is a separate decision that belongs to the screen, not here.
 */
class HasPolymarketDepositNetworkUseCase(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) {

    private val depositNetworkId = PolymarketDepositBlockchain.toNetworkId()

    suspend operator fun invoke(userWalletId: UserWalletId): Boolean {
        val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(userWalletId) ?: return false

        return accountStatusList.flattenCurrencies().any { status ->
            status.currency.network.rawId == depositNetworkId
        }
    }
}