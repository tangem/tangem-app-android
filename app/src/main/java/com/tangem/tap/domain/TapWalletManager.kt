package com.tangem.tap.domain

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.core.analytics.Analytics
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.operations.attestation.Attestation
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletStores.WalletStoresError
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.models.Account
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.disclaimer.createDisclaimer
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class TapWalletManager(
    private val dispatchers: CoroutineDispatcherProvider = AppCoroutineDispatcherProvider(),
) {

    private val blockchainSdkConfig by lazy {
        store.state.globalState.configManager?.config?.blockchainSdkConfig ?: BlockchainSdkConfig()
    }

    private var loadUserWalletDataJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    val walletManagerFactory: WalletManagerFactory
        by lazy { WalletManagerFactory(blockchainSdkConfig) }

    suspend fun onWalletSelected(userWallet: UserWallet, refresh: Boolean, sendAnalyticsEvent: Boolean) {
        // If a previous job was running, it gets cancelled before the new one starts,
        // ensuring that only one job is active at any given time.
        loadUserWalletDataJob = CoroutineScope(dispatchers.io)
            .launch { loadUserWalletData(userWallet, refresh, sendAnalyticsEvent) }
            .also { it.join() }
    }

    private suspend fun loadUserWalletData(userWallet: UserWallet, refresh: Boolean, sendAnalyticsEvent: Boolean) {
        Analytics.setContext(userWallet.scanResponse)
        if (sendAnalyticsEvent) {
            Analytics.send(Basic.WalletOpened())
        }
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
        setupWalletConnectV2(userWallet)
        loadData(userWallet, refresh)
    }

    private fun setupWalletConnectV2(userWallet: UserWallet) {
        val cardId = if (userWallet.scanResponse.card.backupStatus?.isActive != true) {
            userWallet.cardId
        } else { // if wallet has backup, any card from wallet can be used to sign
            null
        }
        scope.launch {
            val wcInteractor = store.state.daggerGraphState.walletConnectInteractor ?: return@launch
            wcInteractor.startListening(
                userWalletId = userWallet.walletId.stringValue,
                cardId = cardId,
            )
        }
    }

    suspend fun loadData(userWallet: UserWallet, refresh: Boolean = false) {
        walletStoresManager.fetch(userWallet, refresh)
            .doOnSuccess {
                Timber.d("Wallet stores fetched for ${userWallet.walletId}")
                store.dispatchOnMain(WalletAction.LoadData.Success)
                store.state.globalState.topUpController?.loadDataSuccess()
                store.dispatchWithMain(WalletAction.Warnings.CheckHashesCount.VerifyOnlineIfNeeded)

                val wcInteractor = store.state.daggerGraphState.walletConnectInteractor
                wcInteractor?.setUserChains(getAccountsForWc(wcInteractor))
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

    private fun getAccountsForWc(wcInteractor: WalletConnectInteractor): List<Account> {
        return store.state.walletState.walletManagers
            .mapNotNull {
                val wallet = it.wallet
                val chainId = wcInteractor.blockchainHelper.networkIdToChainIdOrNull(
                    wallet.blockchain.toNetworkId(),
                )
                chainId?.let {
                    Account(
                        chainId,
                        wallet.address,
                        wallet.publicKey.derivationPath?.rawPath,
                    )
                }
            }
    }

    fun updateConfigManager(data: ScanResponse) {
        val configManager = store.state.globalState.configManager

        if (data.cardTypesResolver.isStart2Coin()) {
            configManager?.turnOff(ConfigManager.IS_TOP_UP_ENABLED)
        } else {
            configManager?.resetToDefault(ConfigManager.IS_TOP_UP_ENABLED)
        }
    }
}

fun Wallet.getFirstToken(): Token? = getTokens().toList().getOrNull(index = 0)
