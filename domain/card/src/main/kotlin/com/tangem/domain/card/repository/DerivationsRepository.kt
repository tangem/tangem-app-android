package com.tangem.domain.card.repository

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.card.BackendId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.ExtendedPublicKeysMap

interface DerivationsRepository {

    @Throws
    suspend fun derivePublicKeys(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

    suspend fun derivePublicKeysByNetworkIds(userWalletId: UserWalletId, networkIds: List<Network.RawID>)

    @Throws
    suspend fun derivePublicKeysByNetworks(userWalletId: UserWalletId, networks: List<Network>)

    @Throws
    suspend fun derivePublicKeys(
        userWalletId: UserWalletId,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Map<ByteArrayKey, ExtendedPublicKeysMap>

    /** Check if user [userWalletId] has missed derivations using map of [Network.ID] with extraDerivationPath */
    suspend fun hasMissedDerivations(
        userWalletId: UserWalletId,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean
}