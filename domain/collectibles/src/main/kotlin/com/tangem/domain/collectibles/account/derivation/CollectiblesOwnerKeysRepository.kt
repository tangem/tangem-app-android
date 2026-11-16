package com.tangem.domain.collectibles.account.derivation

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

/**
 * The owner key of the wallet's collectibles account. Its path ([COLLECTIBLES_OWNER_DERIVATION_PATH]) is reserved
 * and outside the wallet's normal derivation set, so a re-added wallet or a new phone never replays it — the
 * wallet's missing-addresses sync is where it catches up. Missing only while the wallet has a collectibles account.
 */
interface CollectiblesOwnerKeysRepository {

    /**
     * Number of [collectiblesAccounts] whose owner key is not derived on [userWallet] yet. Takes the accounts rather
     * than looking them up, like the joint accounts' owner keys, so the wallet screen counts from the list it holds.
     */
    suspend fun missingOwnerKeysCount(collectiblesAccounts: List<Account.Collectibles>, userWallet: UserWallet): Int

    /** The owner key derivation missing for [userWalletId], without deriving it */
    suspend fun findMissingOwnerKeyDerivation(userWalletId: UserWalletId): Map<ByteArrayKey, List<DerivationPath>>
}