package com.tangem.data.wallets.hot

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.wallets.derivations.MissedDerivationsFinder
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.HotMapDerivationsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.domain.wallets.usecase.BackendId
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal class DefaultHotMapDerivationsRepository @Inject constructor(
    private val networkFactory: NetworkFactory,
    private val hotWalletAccessor: HotWalletAccessor,
    private val dispatchers: CoroutineDispatcherProvider,
) : HotMapDerivationsRepository {

    override suspend fun derivePublicKeys(
        userWallet: UserWallet.Hot,
        currencies: List<CryptoCurrency>,
    ): UserWallet.Hot {
        return derivePublicKeysByNetworks(userWallet = userWallet, networks = currencies.map(CryptoCurrency::network))
    }

    override suspend fun derivePublicKeysByNetworkIds(
        userWallet: UserWallet.Hot,
        networkIds: List<Network.RawID>,
    ): UserWallet.Hot {
        return derivePublicKeysByNetworks(
            userWallet = userWallet,
            networks = networkIds.mapNotNull {
                networkFactory.create(
                    blockchain = Blockchain.fromNetworkId(it.value) ?: return@mapNotNull null,
                    extraDerivationPath = null,
                    userWallet = userWallet,
                )
            },
        )
    }

    override suspend fun derivePublicKeysByNetworks(
        userWallet: UserWallet.Hot,
        networks: List<Network>,
    ): UserWallet.Hot = withContext(dispatchers.default) {
        val derivations = MissedDerivationsFinder(userWallet)
            .findByNetworks(networks)
            .ifEmpty {
                Timber.d("Nothing to derive")
                return@withContext userWallet
            }

        derivePublicKeys(userWallet, derivations).first
    }

    override suspend fun derivePublicKeys(
        userWallet: UserWallet.Hot,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Pair<UserWallet.Hot, Map<ByteArrayKey, ExtendedPublicKeysMap>> {
        val wallets = userWallet.wallets ?: return userWallet to emptyMap()

        val request = DeriveWalletRequest(
            derivations.map { entry ->
                val wallet = wallets.first { it.publicKey.contentEquals(entry.key.bytes) }
                DeriveWalletRequest.Request(
                    curve = wallet.curve,
                    paths = entry.value,
                )
            },
        )
        val result = hotWalletAccessor.derivePublicKeys(
            hotWalletId = userWallet.hotWalletId,
            request = request,
        )
        val newKeys =
            result.responses.associate { ByteArrayKey(it.seedKey.publicKey) to ExtendedPublicKeysMap(it.publicKeys) }

        return userWallet.updateWithNewKeys(newKeys) to newKeys
    }

    override suspend fun hasMissedDerivations(
        userWallet: UserWallet.Hot,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean = withContext(dispatchers.default) {
        val derivations = MissedDerivationsFinder(userWallet)
            .findByNetworks(
                networksWithDerivationPath.mapNotNull { (backendId, extraDerivationPath) ->
                    networkFactory.create(
                        blockchain = Blockchain.fromNetworkId(backendId) ?: return@mapNotNull null,
                        extraDerivationPath = extraDerivationPath,
                        userWallet = userWallet,
                    )
                },
            )

        derivations.isNotEmpty()
    }

    private fun UserWallet.Hot.updateWithNewKeys(newKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>): UserWallet.Hot {
        val wallets = this.wallets ?: return this
        val derivedKeys = wallets.associate {
            it.publicKey.toMapKey() to ExtendedPublicKeysMap(it.derivedKeys)
        }
        val updatedKeys = getUpdatedDerivedKeys(
            oldKeys = derivedKeys,
            newKeys = newKeys,
        )

        return copy(
            wallets = wallets.map { wallet ->
                wallet.copy(
                    derivedKeys = updatedKeys[wallet.publicKey.toMapKey()] ?: ExtendedPublicKeysMap(emptyMap()),
                )
            },
        )
    }

    private fun getUpdatedDerivedKeys(
        oldKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
        newKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
    ): Map<ByteArrayKey, ExtendedPublicKeysMap> {
        return (oldKeys.keys + newKeys.keys).toSet()
            .associateWith { walletKey ->
                val oldDerivations = ExtendedPublicKeysMap(oldKeys[walletKey] ?: emptyMap())
                val newDerivations = newKeys[walletKey] ?: ExtendedPublicKeysMap(emptyMap())

                ExtendedPublicKeysMap(oldDerivations + newDerivations)
            }
    }
}