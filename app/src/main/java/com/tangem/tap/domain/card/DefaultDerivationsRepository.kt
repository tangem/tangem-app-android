package com.tangem.tap.domain.card

import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import timber.log.Timber

internal typealias Derivations = Map<ByteArrayKey, List<DerivationPath>>
private typealias DerivedKeys = Map<ByteArrayKey, ExtendedPublicKeysMap>

internal class DefaultDerivationsRepository(
    private val tangemSdkManager: TangemSdkManager,
    private val userWalletsStore: UserWalletsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : DerivationsRepository {

    override suspend fun derivePublicKeys(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val userWallet = userWalletsStore.getSyncOrNull(userWalletId) ?: error("User wallet not found")

        if (!userWallet.scanResponse.card.settings.isHDWalletAllowed) {
            Timber.d("Nothing to derive")
            return
        }

        val derivations = MissedDerivationsFinder(scanResponse = userWallet.scanResponse)
            .find(currencies)
            .ifEmpty {
                Timber.d("Nothing to derive")
                return
            }

        tangemSdkManager.derivePublicKeys(cardId = null, derivations = derivations)
            .doOnSuccess { response ->
                updatePublicKeys(userWalletId = userWalletId, keys = response.entries).fold(
                    onSuccess = { return },
                    onFailure = { throw it },
                )
            }
            .doOnFailure { throw it }

        error("This code should never be reached")
    }

    private suspend fun updatePublicKeys(userWalletId: UserWalletId, keys: DerivedKeys): Result<Unit> {
        return runCatching(dispatchers.io) {
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
