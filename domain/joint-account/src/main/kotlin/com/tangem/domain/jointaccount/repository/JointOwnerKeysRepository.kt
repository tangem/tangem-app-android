package com.tangem.domain.jointaccount.repository

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

/**
 * The owner keys a wallet has derived for the joint accounts it participates in. A joint account's owner
 * path (`m/44'/60'/888888'/0/{index}`) is reserved and outside the wallet's normal derivation set, so a
 * seed restore or a fresh install never replays it — this is the sync point that catches up.
 */
interface JointOwnerKeysRepository {

    /**
     * Number of distinct owner-key derivations among [jointAccounts] that are missing on [userWallet] —
     * [jointAccounts] sharing the same [Account.Joint.ownerKeyIndex] count once. Takes both as parameters,
     * rather than looking them up by [UserWalletId], so a caller that already holds a fresh,
     * reactively-updated wallet and account list (like the wallet screen's notification pipeline) doesn't
     * pay for a redundant snapshot fetch and stays correctly reactive to derivation changes.
     */
    suspend fun missingOwnerKeysCount(jointAccounts: List<Account.Joint>, userWallet: UserWallet): Int

    /** Finds the joint-account owner-key derivation paths missing for [userWalletId], without deriving them */
    suspend fun findMissingOwnerKeyDerivations(userWalletId: UserWalletId): Map<ByteArrayKey, List<DerivationPath>>
}