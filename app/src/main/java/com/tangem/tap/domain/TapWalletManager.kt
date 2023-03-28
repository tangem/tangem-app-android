package com.tangem.tap.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.core.analytics.Analytics
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.operations.attestation.Attestation
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.walletStores.WalletStoresError
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.disclaimer.createDisclaimer
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.walletStoresManager
import timber.log.Timber

class TapWalletManager {
    val walletManagerFactory: WalletManagerFactory
        by lazy { WalletManagerFactory(blockchainSdkConfig) }

    private val blockchainSdkConfig by lazy {
        store.state.globalState.configManager?.config?.blockchainSdkConfig ?: BlockchainSdkConfig()
    }

    suspend fun onWalletSelected(userWallet: UserWallet, refresh: Boolean) {
        Analytics.setContext(userWallet.scanResponse)
        val scanResponse = userWallet.scanResponse
        val card = scanResponse.card
        val attestationFailed = card.attestation.status == Attestation.Status.Failed

        tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)
        store.state.globalState.feedbackManager?.infoHolder?.setCardInfo(scanResponse)
        updateConfigManager(scanResponse)
        withMainContext {
            // Order is important
            store.dispatch(DisclaimerAction.SetDisclaimer(card.createDisclaimer()))
            store.dispatch(WalletAction.UserWalletChanged(userWallet))
            store.dispatch(WalletAction.UpdateCanSaveUserWallets(preferencesStorage.shouldSaveUserWallets))
            store.dispatch(TwinCardsAction.IfTwinsPrepareState(scanResponse))
            store.dispatch(WalletConnectAction.ResetState)
            store.dispatch(GlobalAction.SaveScanResponse(scanResponse))
            store.dispatch(WalletConnectAction.RestoreSessions(scanResponse))
            store.dispatch(GlobalAction.SetIfCardVerifiedOnline(!attestationFailed))
            store.dispatch(WalletAction.Warnings.CheckIfNeeded)
        }

        loadData(userWallet, refresh)
    }

    suspend fun loadData(userWallet: UserWallet, refresh: Boolean = false) {
        walletStoresManager.fetch(userWallet, refresh)
            .doOnSuccess {
                Timber.d("Wallet stores fetched for ${userWallet.walletId}")
                store.dispatchOnMain(WalletAction.LoadData.Success)
                store.state.globalState.topUpController?.loadDataSuccess()
            }
            .doOnFailure { error ->
                val errorAction = when (error) {
                    is WalletStoresError -> when (error) {
                        is WalletStoresError.FetchFiatRatesError -> WalletAction.LoadData.Failure(error = null)
                        is WalletStoresError.UpdateWalletManagerTokensError -> WalletAction.LoadData.Failure(
                            error = TapError.WalletManager.InternalError(
                                message = error.cause.localizedMessage ?: error.customMessage,
                            ),
                        )
                        is WalletStoresError.WalletManagerNotCreated -> WalletAction.LoadData.Failure(
                            error = TapError.WalletManager.CreationError,
                        )
                        is WalletStoresError.UnknownBlockchain -> WalletAction.LoadData.Failure(
                            error = TapError.UnknownBlockchain,
                        )
                        is WalletStoresError.NoInternetConnection -> WalletAction.LoadData.Failure(
                            error = TapError.NoInternetConnection,
                        )
                    }
                    else -> WalletAction.LoadData.Failure(error = null)
                }

                Timber.e(error, "Wallet stores fetching failed for ${userWallet.walletId}")

                store.dispatchOnMain(errorAction)
            }
    }

    fun updateConfigManager(data: ScanResponse) {
        val configManager = store.state.globalState.configManager
        val blockchain = data.cardTypesResolver.getBlockchain()
        if (data.cardTypesResolver.isStart2Coin()) {
            configManager?.turnOff(ConfigManager.IS_SENDING_TO_PAY_ID_ENABLED)
            configManager?.turnOff(ConfigManager.IS_TOP_UP_ENABLED)
        } else if (blockchain == Blockchain.Bitcoin ||
            data.walletData?.blockchain == Blockchain.Bitcoin.id
        ) {
            configManager?.resetToDefault(ConfigManager.IS_SENDING_TO_PAY_ID_ENABLED)
            configManager?.resetToDefault(ConfigManager.IS_TOP_UP_ENABLED)
        } else {
            configManager?.resetToDefault(ConfigManager.IS_SENDING_TO_PAY_ID_ENABLED)
            configManager?.resetToDefault(ConfigManager.IS_TOP_UP_ENABLED)
        }
    }
}

fun Wallet.getFirstToken(): Token? {
    return getTokens().toList().getOrNull(0)
}
