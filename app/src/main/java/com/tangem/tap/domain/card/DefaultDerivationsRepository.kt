package com.tangem.tap.domain.card

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.common.currency.getNetwork
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.BackendId
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.tasks.UserWalletIdPreflightReadFilter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

internal typealias Derivations = Map<ByteArrayKey, List<DerivationPath>>
private typealias DerivedKeys = Map<ByteArrayKey, ExtendedPublicKeysMap>

internal class DefaultDerivationsRepository(
    private val tangemSdkManager: TangemSdkManager,
    private val userWalletsStore: UserWalletsStore,
    private val excludedBlockchains: ExcludedBlockchains,
    private val dispatchers: CoroutineDispatcherProvider,
) : DerivationsRepository {

    override suspend fun derivePublicKeys(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        derivePublicKeysByNetworks(userWalletId = userWalletId, networks = currencies.map(CryptoCurrency::network))
    }

    override suspend fun derivePublicKeysByNetworkIds(userWalletId: UserWalletId, networkIds: List<Network.RawID>) {
        val userWallet = userWalletsStore.getSyncOrNull(userWalletId) ?: error("User wallet not found")

        derivePublicKeysByNetworks(
            userWalletId = userWalletId,
            networks = networkIds.mapNotNull {
                getNetwork(
                    blockchain = Blockchain.fromNetworkId(it.value) ?: return@mapNotNull null,
                    extraDerivationPath = null,
                    scanResponse = userWallet.scanResponse,
                    excludedBlockchains = excludedBlockchains,
                )
            },
        )
    }

    override suspend fun derivePublicKeysByNetworks(userWalletId: UserWalletId, networks: List<Network>) {
        val userWallet = withContext(dispatchers.io) {
            userWalletsStore.getSyncOrNull(userWalletId) ?: error("User wallet not found")
        }

        if (!userWallet.scanResponse.card.settings.isHDWalletAllowed) {
            Timber.d("Nothing to derive")
            return
        }

        val derivations = MissedDerivationsFinder(scanResponse = userWallet.scanResponse)
            .findByNetworks(networks)
            .ifEmpty {
                Timber.d("Nothing to derive")
                return
            }

        derivePublicKeys(userWalletId = userWalletId, derivations = derivations)
    }

    override suspend fun hasMissedDerivations(
        userWalletId: UserWalletId,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean {
        val userWallet = userWalletsStore.getSyncOrNull(userWalletId) ?: error("User wallet not found")

        val derivations = MissedDerivationsFinder(scanResponse = userWallet.scanResponse)
            .findByNetworks(
                networksWithDerivationPath.mapNotNull { (backendId, extraDerivationPath) ->
                    getNetwork(
                        blockchain = Blockchain.fromNetworkId(backendId) ?: return@mapNotNull null,
                        extraDerivationPath = extraDerivationPath,
                        scanResponse = userWallet.scanResponse,
                        excludedBlockchains = excludedBlockchains,
                    )
                },
            )

        return derivations.isNotEmpty()
    }

    override suspend fun derivePublicKeys(userWalletId: UserWalletId, derivations: Derivations): DerivedKeys {
        // todo replace it in task [REDACTED_JIRA]
        val preflightReadFilter = UserWalletIdPreflightReadFilter(userWalletId)
        tangemSdkManager.derivePublicKeys(
            cardId = null,
            derivations = derivations,
            preflightReadFilter = preflightReadFilter,
        ).doOnSuccess { response ->
            updatePublicKeys(userWalletId = userWalletId, keys = response.entries)
                .doOnSuccess {
                    validateDerivations(scanResponse = it.scanResponse, derivations = derivations)
                    return response.entries
                }
                .doOnFailure { throw it }
        }
            .doOnFailure { throw it }

        error("This code should never be reached")
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

    private suspend fun updatePublicKeys(userWalletId: UserWalletId, keys: DerivedKeys): CompletionResult<UserWallet> {
        return withContext(dispatchers.io) {
            userWalletsStore.update(
                userWalletId = userWalletId,
                update = { userWallet -> userWallet.updateDerivedKeys(keys) },
            )
        }
    }

    private fun UserWallet.updateDerivedKeys(keys: DerivedKeys): UserWallet {
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