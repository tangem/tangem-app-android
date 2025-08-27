package com.tangem.domain.wallets.derivations

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.BackendId
import com.tangem.operations.derivation.ExtendedPublicKeysMap

interface ColdMapDerivationsRepository {

    @Throws
    suspend fun derivePublicKeys(userWallet: UserWallet.Cold, currencies: List<CryptoCurrency>): UserWallet.Cold

    suspend fun derivePublicKeysByNetworkIds(
        userWallet: UserWallet.Cold,
        networkIds: List<Network.RawID>,
    ): UserWallet.Cold

    @Throws
    suspend fun derivePublicKeysByNetworks(userWallet: UserWallet.Cold, networks: List<Network>): UserWallet.Cold

    @Throws
    suspend fun derivePublicKeys(
        userWallet: UserWallet.Cold,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Pair<UserWallet.Cold, Map<ByteArrayKey, ExtendedPublicKeysMap>>

    /** Check if user [userWallet] has missed derivations using map of [Network.ID] with extraDerivationPath */
    suspend fun hasMissedDerivations(
        userWallet: UserWallet.Cold,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean
}