package com.tangem.domain.jointaccount.verifier

import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.jointaccount.safe.SafeSingleton
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.logging.TangemLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Checks the composition the backend states every time it arrives: this wallet's owner address — derived from the
 * wallet, never taken from the backend — must be among the members, and once there is a Safe address it must follow
 * from the members and the threshold.
 */
class JointAccountCompositionVerifier(
    private val ownerAddressResolver: JointOwnerAddressResolver,
    private val safeAddressCalculator: SafeAddressCalculator,
) {

    private val ownerAddresses = ConcurrentHashMap<Pair<UserWalletId, OwnerKeyIndex>, String>()

    suspend fun isVerified(
        userWalletId: UserWalletId,
        ownerKeyIndex: OwnerKeyIndex,
        jointAccount: JointAccount,
    ): Boolean {
        // Membership cannot be confirmed without the wallet's own address, so the account is not trusted
        val ownerAddress = ownerAddress(userWalletId = userWalletId, ownerKeyIndex = ownerKeyIndex) ?: return false

        val isMember = jointAccount.members.any { it.address.equals(ownerAddress, ignoreCase = true) }
        // Only a pending account has no Safe address yet; once the composition is full one is always stated
        val safeAddress = jointAccount.safeAddress
            ?: return isMember && jointAccount.status == JointAccount.Status.PENDING

        return isMember && isSafeAddressComputed(jointAccount = jointAccount, safeAddress = safeAddress)
    }

    private suspend fun ownerAddress(userWalletId: UserWalletId, ownerKeyIndex: OwnerKeyIndex): String? {
        val key = userWalletId to ownerKeyIndex
        ownerAddresses[key]?.let { return it }

        return ownerAddressResolver.resolve(userWalletId = userWalletId, ownerKeyIndex = ownerKeyIndex)
            .onLeft { TangemLogger.e("Unable to derive the joint owner address of $userWalletId", it) }
            .getOrNull()
            ?.also { ownerAddresses[key] = it }
    }

    private fun isSafeAddressComputed(jointAccount: JointAccount, safeAddress: String): Boolean {
        val owners = jointAccount.members.map(JointAccount.Member::address)

        return SafeSingleton.entries.any { singleton ->
            safeAddressCalculator.compute(owners = owners, threshold = jointAccount.threshold, singleton = singleton)
                .getOrNull()
                ?.equals(safeAddress, ignoreCase = true) == true
        }
    }
}