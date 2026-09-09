package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.assetsdiscovery.usecase.StartAssetsDiscoveryUseCase
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.models.WalletSyncResult
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.wallets.usecase.SyncWalletWithRemoteUseCase
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.exception.PassphraseTooLongException
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Why a [Mnemonic] could not be turned into a saved wallet */
internal sealed interface HotWalletImportError {

    /** The very same wallet is already in the wallets list */
    data object AlreadySaved : HotWalletImportError

    /** The passphrase exceeds [maxByteCount] UTF-8 bytes in NFKD form */
    data class PassphraseTooLong(val maxByteCount: Int) : HotWalletImportError

    data class Unknown(val cause: Throwable? = null) : HotWalletImportError
}

/**
 * Shared "materialize a [Mnemonic] into a saved hot [UserWallet.Hot]" step.
 *
 * Extracted verbatim from the seed-import flow so the recovery-phrase import and the cloud-backup
 * restore converge on the exact same save/sync/analytics sequence: import via the Hot SDK, build the
 * wallet, persist it, kick off the remote sync + assets discovery, and send the identical onboarding
 * analytics ([AnalyticsParam.WalletCreationType.SeedImport]). Callers own only their progress UI, the
 * navigation callback and the seed-phrase backup state they import with.
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

    /**
     * @param scope outlives this call: the remote sync it starts must survive the caller's screen
     * @param isSeedPhraseBackedUp whether the user already holds the recovery phrase. True when they typed
     * it in themselves, false when the phrase came from a backup they never saw (e.g. a cloud restore).
     */
    suspend fun import(
        scope: CoroutineScope,
        mnemonic: Mnemonic,
        passphrase: CharArray?,
        name: String? = null,
        isSeedPhraseBackedUp: Boolean,
    ): Either<HotWalletImportError, UserWalletId> = either {
        val userWallet = runSuspendCatching {
            val hotWalletId = tangemHotSdk.importWallet(mnemonic, passphrase, HotAuth.NoAuth)
            hotUserWalletBuilderFactory.create(hotWalletId).build(name = name)
        }.getOrElse { error ->
            TangemLogger.e("Unable to import the wallet", error)
            when (error) {
                is PassphraseTooLongException -> raise(
                    HotWalletImportError.PassphraseTooLong(maxByteCount = error.maxByteCount),
                )
                else -> raise(HotWalletImportError.Unknown(error))
            }
        }

        save(userWallet, isSeedPhraseBackedUp).bind()
        startRemoteSync(scope, userWallet.walletId)
        sendWalletCreatedEvents(mnemonic, passphrase)

        userWallet.walletId
    }

    private suspend fun save(
        userWallet: UserWallet.Hot,
        isSeedPhraseBackedUp: Boolean,
    ): Either<HotWalletImportError, Unit> =
        saveUserWalletUseCase.invoke(userWallet.copy(backedUp = isSeedPhraseBackedUp)).mapLeft { error ->
            when (error) {
                is SaveWalletError.DataError -> {
                    TangemLogger.e("Unable to save user wallet: $error")
                    HotWalletImportError.Unknown()
                }
                is SaveWalletError.WalletAlreadySaved -> HotWalletImportError.AlreadySaved
            }
        }.map { }

    private fun startRemoteSync(scope: CoroutineScope, userWalletId: UserWalletId) {
        scope.launch(dispatchers.main + NonCancellable) {
            val syncResult = syncWalletWithRemoteUseCase(userWalletId)
            if (syncResult == WalletSyncResult.Created) {
                startAssetsDiscoveryUseCase(userWalletId)
            }
        }
    }

    private suspend fun sendWalletCreatedEvents(mnemonic: Mnemonic, passphrase: CharArray?) {
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
    }
}