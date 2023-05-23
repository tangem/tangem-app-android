package com.tangem.tap.features.customtoken.impl.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.guard
import com.tangem.common.flatMap
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.models.Currency.NativeToken
import com.tangem.lib.crypto.models.Currency.NonNativeToken
import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.scope
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletCurrenciesManager
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Default implementation of custom token interactor
 *
 * @property featureRepository feature repository
 * @property derivationManager derivation manager
 * @property reduxStateHolder  redux state holder
 *
[REDACTED_AUTHOR]
 */
class DefaultCustomTokenInteractor(
    private val featureRepository: CustomTokenRepository,
    private val derivationManager: DerivationManager,
    private val reduxStateHolder: AppStateHolder,
) : CustomTokenInteractor {

    override suspend fun findToken(address: String, blockchain: Blockchain): FoundToken {
        return featureRepository.findToken(
            address = address,
            networkId = if (blockchain != Blockchain.Unknown) blockchain.toNetworkId() else null,
        )
    }

    override suspend fun saveToken(currency: Currency, address: String) {
        val hasDerivation = derivationManager.hasDerivation(
            networkId = currency.blockchain.toNetworkId(),
            derivationPath = requireNotNull(currency.derivationPath),
        )

        if (!hasDerivation) {
            derivationManager.deriveMissingBlockchains(
                when (currency) {
                    is Currency.Blockchain -> NativeToken(
                        id = requireNotNull(currency.coinId),
                        name = currency.currencyName,
                        symbol = currency.currencySymbol,
                        networkId = currency.blockchain.toNetworkId(),
                    )

                    is Currency.Token -> NonNativeToken(
                        id = requireNotNull(currency.coinId),
                        name = currency.currencyName,
                        symbol = currency.currencySymbol,
                        networkId = currency.blockchain.toNetworkId(),
                        contractAddress = address,
                        decimalCount = currency.decimals,
                    )
                },
            )
        }

        submitAdd(
            scanResponse = requireNotNull(reduxStateHolder.scanResponse),
            currency = currency,
        )
    }

    private fun submitAdd(scanResponse: ScanResponse, currency: Currency) {
        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to add currencies, no user wallet selected")
            return
        }
        scope.launch {
            userWalletsListManager.update(
                userWalletId = selectedUserWallet.walletId,
                update = { userWallet ->
                    userWallet.copy(scanResponse = scanResponse)
                },
            )
                .flatMap { updatedUserWallet ->
                    walletCurrenciesManager.addCurrencies(
                        userWallet = updatedUserWallet,
                        currenciesToAdd = listOf(currency),
                    )
                }
        }
    }
}