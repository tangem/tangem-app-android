package com.tangem.domain.managetokens

import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.jointaccount.repository.JointAccountSupportedNetworksRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.network.Network
import kotlin.error

/**
 * Resolves the set of networks an account is restricted to when managing tokens.
 *
 * Returns `null` when the account has no network restriction (regular accounts), or the joint
 * account allowlist when the account is a [Account.Joint].
 *
 * Waits for the account to actually resolve (rather than peeking at a possibly-stale cache) so a
 * not-yet-loaded [Account.Joint] can't be mistaken for "no restriction".
 */
class GetManageTokensAllowedNetworksUseCase(
    private val singleAccountSupplier: SingleAccountSupplier,
    private val jointAccountSupportedNetworksRepository: JointAccountSupportedNetworksRepository,
) {

    suspend operator fun invoke(accountId: AccountId): Set<Network.RawID>? {
        val account = singleAccountSupplier
            .getSyncOrNull(SingleAccountProducer.Params(accountId)) ?: error("Account not found: $accountId")

        return when (account) {
            is Account.Joint -> jointAccountSupportedNetworksRepository.getSupportedNetworks().toSet()
            else -> null
        }
    }
}