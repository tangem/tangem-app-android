package com.tangem.data.jointaccount.address

import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.domain.jointaccount.derivation.toNetworkDerivationPath
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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

        val wantedPaths = jointNetworks.map(Network::derivationPath).toSet()
        val safeAddressByPath = withTimeoutOrNull(AWAIT_TIMEOUT) {
            awaitSafeAddressByPath(userWalletId = userWalletId, wantedPaths = wantedPaths)
        }.orEmpty()

        return jointNetworks
            .mapNotNull { network -> safeAddressByPath[network.derivationPath]?.let { network to it } }
            .toMap()
    }

    /**
     * Waits for an address of one of [wantedPaths] rather than for any address at all: another joint account
     * already having one says nothing about the account being asked about, and stopping there would answer
     * "no address" for an account whose address is still on its way.
     *
     * The two sources are combined rather than read one after the other, so an address arriving after the first
     * snapshot is still seen — reading them in sequence would pin a stale half.
     */
    private suspend fun awaitSafeAddressByPath(
        userWalletId: UserWalletId,
        wantedPaths: Set<Network.DerivationPath>,
    ): Map<Network.DerivationPath, String> {
        return combine(
            flow = jointAccountsStore.get(userWalletId).map { it.orEmpty().safeAddressByAccountId() },
            flow2 = walletAccountsFetcher.get(userWalletId),
        ) { safeAddressByAccountId, response ->
            response.accounts.safeAddressByPath(safeAddressByAccountId)
        }
            .first { byPath -> wantedPaths.any(byPath::containsKey) }
    }

    private fun List<JointAccount>.safeAddressByAccountId(): Map<String, String> = mapNotNull { account ->
        account.safeAddress?.let { account.cryptoAccountId to it }
    }.toMap()

    private fun List<WalletAccountDTO>.safeAddressByPath(
        safeAddressByAccountId: Map<String, String>,
    ): Map<Network.DerivationPath, String> = filter { it.type == WalletAccountDTO.Type.JOINT }
        .mapNotNull { account ->
            val path = OwnerKeyIndex(account.derivationIndex).getOrNull()?.toNetworkDerivationPath()
            val safeAddress = safeAddressByAccountId[account.id]

            if (path != null && safeAddress != null) path to safeAddress else null
        }
        .toMap()

    private companion object {
        val AWAIT_TIMEOUT = 5.seconds
    }
}