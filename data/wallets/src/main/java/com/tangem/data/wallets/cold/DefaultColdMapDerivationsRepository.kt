package com.tangem.data.wallets.cold

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.wallets.derivations.Derivations
import com.tangem.data.wallets.derivations.MissedDerivationsFinder
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.ColdMapDerivationsRepository
import com.tangem.domain.wallets.usecase.BackendId
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

private typealias DerivedKeys = Map<ByteArrayKey, ExtendedPublicKeysMap>

internal class DefaultColdMapDerivationsRepository @Inject constructor(
    private val tangemSdkManager: TangemSdkManager,
    private val networkFactory: NetworkFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : ColdMapDerivationsRepository {

    override suspend fun derivePublicKeys(
        userWallet: UserWallet.Cold,
        currencies: List<CryptoCurrency>,
    ): UserWallet.Cold = withContext(dispatchers.io) {
        derivePublicKeysByNetworks(userWallet = userWallet, networks = currencies.map(CryptoCurrency::network))
    }

    override suspend fun derivePublicKeysByNetworkIds(
        userWallet: UserWallet.Cold,
        networkIds: List<Network.RawID>,
    ): UserWallet.Cold = withContext(dispatchers.io) {
        derivePublicKeysByNetworks(
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
        userWallet: UserWallet.Cold,
        networks: List<Network>,
    ): UserWallet.Cold = withContext(dispatchers.io) {
        if (!userWallet.scanResponse.card.settings.isHDWalletAllowed) {
            Timber.d("Nothing to derive")
            return@withContext userWallet
        }

        val derivations = MissedDerivationsFinder(userWallet)
            .findByNetworks(networks)
            .ifEmpty {
                Timber.d("Nothing to derive")
                return@withContext userWallet
            }

        return@withContext derivePublicKeys(userWallet = userWallet, derivations = derivations).first
    }

    override suspend fun derivePublicKeys(
        userWallet: UserWallet.Cold,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Pair<UserWallet.Cold, Map<ByteArrayKey, ExtendedPublicKeysMap>> = withContext(dispatchers.io) {
        // todo replace it in task [REDACTED_JIRA]
        val preflightReadFilter = UserWalletIdPreflightReadFilter(userWallet.walletId)
        val result = tangemSdkManager.derivePublicKeys(
            cardId = null,
            derivations = derivations,
            preflightReadFilter = preflightReadFilter,
        )

        when (result) {
            is CompletionResult.Success -> {
                userWallet.updateDerivedKeys(result.data.entries).also {
                    validateDerivations(scanResponse = it.scanResponse, derivations = derivations)
                } to result.data.entries
            }
            is CompletionResult.Failure -> {
                throw result.error
            }
        }
    }

    override suspend fun hasMissedDerivations(
        userWallet: UserWallet.Cold,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean = withContext(dispatchers.io) {
        val derivations =
            MissedDerivationsFinder(userWallet)
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

    /**
     * It throws an exception if any of the provided derivations are invalid
     * Validation for NonHardened moved to application layer, to avoid fails when derive multiple paths
     * It needs to be called after success [derivePublicKeys] or in same flows
     */
    private fun validateDerivations(scanResponse: ScanResponse, derivations: Derivations) {
        derivations.entries.forEach { derivationForKey ->
            val wallet = scanResponse.card.wallets.firstOrNull { it.publicKey.toMapKey() == derivationForKey.key }
            if (wallet == null) return@forEach
            val hasHardenedNodes = derivationForKey.value.any { path -> path.nodes.any { node -> !node.isHardened } }
            if (wallet.curve == EllipticCurve.Ed25519Slip0010 && hasHardenedNodes) {
                throw TangemSdkError.NonHardenedDerivationNotSupported()
            }
        }
    }

    private fun UserWallet.Cold.updateDerivedKeys(keys: DerivedKeys): UserWallet.Cold {
        return copy(
            scanResponse = scanResponse.copy(
                derivedKeys = getUpdatedDerivedKeys(oldKeys = scanResponse.derivedKeys, newKeys = keys),
            ),
        )
    }

    private fun getUpdatedDerivedKeys(oldKeys: DerivedKeys, newKeys: DerivedKeys): DerivedKeys {
        return (oldKeys.keys + newKeys.keys).toSet()
            .associateWith { walletKey ->
                val oldDerivations = ExtendedPublicKeysMap(oldKeys[walletKey] ?: emptyMap())
                val newDerivations = newKeys[walletKey] ?: ExtendedPublicKeysMap(emptyMap())

                ExtendedPublicKeysMap(oldDerivations + newDerivations)
            }
    }
}