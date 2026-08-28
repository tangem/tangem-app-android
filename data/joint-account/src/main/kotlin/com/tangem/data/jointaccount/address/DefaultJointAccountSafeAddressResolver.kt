package com.tangem.data.jointaccount.address

import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.domain.jointaccount.derivation.toNetworkDerivationPath
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * Joins the two sources a joint account is split across: the accounts data names the account and its owner key
 * index (hence the network path), the joint accounts store knows its Safe address.
 */
internal class DefaultJointAccountSafeAddressResolver @Inject constructor(
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val jointAccountsStore: JointAccountsStore,
) : JointAccountSafeAddressResolver {

    override suspend fun resolve(userWalletId: UserWalletId, networks: Set<Network>): Map<Network, String> {
        val jointNetworks = networks.filter { it.derivationPath is Network.DerivationPath.Joint }
        if (jointNetworks.isEmpty()) return emptyMap()

        val safeAddressByAccountId = jointAccountsStore.getSyncOrNull(userWalletId).orEmpty()
            .mapNotNull { account -> account.safeAddress?.let { account.cryptoAccountId to it } }
            .toMap()
        if (safeAddressByAccountId.isEmpty()) return emptyMap()

        val safeAddressByPath = walletAccountsFetcher.getSaved(userWalletId)?.accounts.orEmpty()
            .filter { it.type == WalletAccountDTO.Type.JOINT }
            .mapNotNull { account ->
                val path = OwnerKeyIndex(account.derivationIndex).getOrNull()?.toNetworkDerivationPath()
                val safeAddress = safeAddressByAccountId[account.id]

                if (path != null && safeAddress != null) path to safeAddress else null
            }
            .toMap()

        return jointNetworks
            .mapNotNull { network -> safeAddressByPath[network.derivationPath]?.let { network to it } }
            .toMap()
    }
}