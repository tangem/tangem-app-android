package com.tangem.domain.jointaccount.verifier

import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfirmationPayload
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.jointaccount.safe.SafeSingleton
import com.tangem.domain.jointaccount.signing.JointSignatureRecoverer
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.logging.TangemLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Checks the composition the backend states every time it arrives: this wallet's owner address — derived from the
 * wallet, never taken from the backend — must be among the members; once there is a Safe address it must follow
 * from the members and the threshold; and every confirmation signature must recover to the member it is attributed
 * to, over the payload this wallet signs itself.
 */
class JointAccountCompositionVerifier(
    private val ownerAddressResolver: JointOwnerAddressResolver,
    private val safeAddressCalculator: SafeAddressCalculator,
    private val signatureRecoverer: JointSignatureRecoverer,
) {

    private val ownerAddresses = ConcurrentHashMap<Pair<UserWalletId, OwnerKeyIndex>, String>()

    suspend fun isVerified(account: Account.Joint, jointAccount: JointAccount): Boolean {
        // Membership cannot be confirmed without the wallet's own address, so the account is not trusted
        val ownerAddress = ownerAddress(
            userWalletId = account.accountId.userWalletId,
            ownerKeyIndex = account.ownerKeyIndex,
        ) ?: return false

        val isMember = jointAccount.members.any { it.address.equals(ownerAddress, ignoreCase = true) }
        // Only a pending account has no Safe address yet; once the composition is full one is always stated
        val safeAddress = jointAccount.safeAddress
            ?: return isMember && isUnsignedPending(jointAccount)

        return isMember &&
            isSafeAddressComputed(jointAccount = jointAccount, safeAddress = safeAddress) &&
            areSignaturesVerified(account = account, jointAccount = jointAccount, safeAddress = safeAddress)
    }

    suspend fun ownerAddress(userWalletId: UserWalletId, ownerKeyIndex: OwnerKeyIndex): String? {
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

    /** Nobody can confirm before the Safe address exists, so a signature on a pending account is out of contract */
    private fun isUnsignedPending(jointAccount: JointAccount): Boolean {
        if (jointAccount.status != JointAccount.Status.PENDING) return false

        val signed = jointAccount.members.count { it.signature != null }
        if (signed > 0) {
            TangemLogger.e("Joint account ${jointAccount.cryptoAccountId} is pending with $signed signed members")
        }
        return signed == 0
    }

    /** A wrong signature fails in any status; a missing one only once the account is active, i.e. fully confirmed */
    private fun areSignaturesVerified(
        account: Account.Joint,
        jointAccount: JointAccount,
        safeAddress: String,
    ): Boolean {
        val signed = jointAccount.members.filter { it.signature != null }

        if (jointAccount.status == JointAccount.Status.ACTIVE && signed.size != jointAccount.members.size) {
            TangemLogger.e(
                "Joint account ${jointAccount.cryptoAccountId} is active with ${signed.size} of " +
                    "${jointAccount.members.size} members confirmed",
            )
            return false
        }
        if (signed.isEmpty()) return true

        val payload = JointAccountConfirmationPayload.of(
            account = account,
            jointAccount = jointAccount,
            safeAddress = safeAddress,
        )
        if (payload == null) {
            TangemLogger.e("Joint account ${jointAccount.cryptoAccountId} carries the default main account name")
            return false
        }

        return signed.all { member -> isSignedBy(member = member, payload = payload, jointAccount = jointAccount) }
    }

    private fun isSignedBy(
        member: JointAccount.Member,
        payload: JointAccountConfirmationPayload,
        jointAccount: JointAccount,
    ): Boolean {
        val signature = member.signature ?: return false
        val signer = signatureRecoverer.recoverPayloadSigner(payload = payload, signature = signature)
            .onLeft { error ->
                TangemLogger.e(
                    "The confirmation signature of ${member.address} in ${jointAccount.cryptoAccountId} " +
                        "does not recover",
                    error,
                )
            }
            .getOrNull()
            ?: return false

        val isOwn = signer.equals(member.address, ignoreCase = true)
        if (!isOwn) {
            TangemLogger.e(
                "The confirmation signature of ${member.address} in ${jointAccount.cryptoAccountId} " +
                    "recovers to $signer",
            )
        }

        return isOwn
    }
}