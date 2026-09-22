package com.tangem.data.jointaccount.signing

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.usecase.primarySecp256k1PublicKey
import com.tangem.domain.wallets.derivations.DerivationsRepository
import javax.inject.Inject

internal class JointOwnerKeyReader @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
) {

    class OwnerKey(
        val userWallet: UserWallet,
        val seedKey: ByteArray,
        val path: DerivationPath,
        val extendedPublicKey: ExtendedPublicKey,
    )

    suspend fun read(userWalletId: UserWalletId, ownerKeyIndex: OwnerKeyIndex): OwnerKey? {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val seedKey = checkNotNull(userWallet.primarySecp256k1PublicKey()) { "The wallet has no secp256k1 seed key" }

        val ownerPath = jointAccountOwnerDerivationPath(index = ownerKeyIndex.value)
        val derivedKeys = derivationsRepository
            .getExistingDerivedKeys(userWalletId = userWalletId, seedKey = ByteArrayKey(seedKey))
        val ownerKey = derivedKeys[ownerPath] ?: return null

        return OwnerKey(userWallet = userWallet, seedKey = seedKey, path = ownerPath, extendedPublicKey = ownerKey)
    }
}