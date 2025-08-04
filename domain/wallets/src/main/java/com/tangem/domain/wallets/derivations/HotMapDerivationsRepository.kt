package com.tangem.domain.wallets.derivations

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.BackendId
import com.tangem.operations.derivation.ExtendedPublicKeysMap

interface HotMapDerivationsRepository {

    @Throws
    suspend fun derivePublicKeys(userWallet: UserWallet.Hot, currencies: List<CryptoCurrency>): UserWallet.Hot

    suspend fun derivePublicKeysByNetworkIds(
        userWallet: UserWallet.Hot,
        networkIds: List<Network.RawID>,
    ): UserWallet.Hot

    @Throws
    suspend fun derivePublicKeysByNetworks(userWallet: UserWallet.Hot, networks: List<Network>): UserWallet.Hot

    @Throws
    suspend fun derivePublicKeys(
        userWallet: UserWallet.Hot,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Pair<UserWallet.Hot, Map<ByteArrayKey, ExtendedPublicKeysMap>>

    /** Check if user [userWallet] has missed derivations using map of [Network.ID] with extraDerivationPath */
    suspend fun hasMissedDerivations(
        userWallet: UserWallet.Hot,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean
}