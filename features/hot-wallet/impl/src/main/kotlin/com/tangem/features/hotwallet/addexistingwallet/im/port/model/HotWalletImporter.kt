package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.assetsdiscovery.usecase.StartAssetsDiscoveryUseCase
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.models.WalletSyncResult
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.wallets.usecase.SyncWalletWithRemoteUseCase
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared "materialize a [Mnemonic] into a saved hot [com.tangem.domain.models.wallet.UserWallet.Hot]" step.
 *
 * Extracted verbatim from the seed-import flow so the recovery-phrase import and the cloud-backup
 * restore converge on the exact same save/sync/analytics sequence: import via the Hot SDK, build the
 * wallet, persist it as already `backedUp = true`, kick off the remote sync + assets discovery, and
 * send the identical onboarding analytics ([AnalyticsParam.WalletCreationType.SeedImport]). Callers own
 * only their progress UI and the navigation callback via the returned [Result].
 */
@Suppress("LongParameterList")
internal class HotWalletImporter @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveUserWalletUseCase: SaveWalletUseCase,
    private val syncWalletWithRemoteUseCase: SyncWalletWithRemoteUseCase,
    private val startAssetsDiscoveryUseCase: StartAssetsDiscoveryUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appsFlyerStore: AppsFlyerStore,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    sealed interface Result {
        data class Success(val userWalletId: UserWalletId) : Result
        data object AlreadySaved : Result
        data object Failure : Result
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun import(
        scope: CoroutineScope,
        mnemonic: Mnemonic,
        passphrase: CharArray?,
        name: String? = null,
    ): Result {
        return try {
            val hotWalletId = tangemHotSdk.importWallet(mnemonic, passphrase, HotAuth.NoAuth)
            val userWallet = hotUserWalletBuilderFactory.create(hotWalletId).build(name = name)
            saveUserWalletUseCase.invoke(userWallet.copy(backedUp = true)).fold(
                ifLeft = { error ->
                    when (error) {
                        is SaveWalletError.DataError -> {
                            TangemLogger.e("Unable to save user wallet: $error")
                            Result.Failure
                        }
                        is SaveWalletError.WalletAlreadySaved -> Result.AlreadySaved
                    }
                },
                ifRight = {
                    scope.launch(dispatchers.main + NonCancellable) {
                        val syncResult = syncWalletWithRemoteUseCase(userWallet.walletId)
                        if (syncResult == WalletSyncResult.Created) {
                            startAssetsDiscoveryUseCase(userWallet.walletId)
                        }
                    }

                    analyticsEventHandler.send(
                        event = OnboardingAnalyticsEvent.Onboarding.Finished(
                            source = AnalyticsParam.ScreensSources.ImportWallet,
                        ),
                    )
                    analyticsEventHandler.send(
                        event = OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully(
                            source = AnalyticsParam.ScreensSources.ImportWallet,
                            creationType = AnalyticsParam.WalletCreationType.SeedImport,
                            seedPhraseLength = mnemonic.mnemonicComponents.size,
                            passPhraseState = if (passphrase?.any { char -> !char.isWhitespace() } == true) {
                                AnalyticsParam.EmptyFull.Full
                            } else {
                                AnalyticsParam.EmptyFull.Empty
                            },
                            referralId = appsFlyerStore.get()?.refcode,
                        ),
                    )
                    Result.Success(userWallet.walletId)
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TangemLogger.e("Error", e)
            Result.Failure
        }
    }
}