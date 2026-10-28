package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.fx.coroutines.onCancel
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.assetsdiscovery.usecase.StartAssetsDiscoveryUseCase
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.models.HotWalletAdditionRestriction
import com.tangem.domain.wallets.models.WalletSyncResult
import com.tangem.domain.wallets.usecase.DiscardUnsavedHotWalletUseCase
import com.tangem.domain.wallets.usecase.GetHotWalletAdditionRestrictionUseCase
import com.tangem.domain.wallets.usecase.IsWalletAlreadySavedUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.wallets.usecase.SyncWalletWithRemoteUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.exception.PassphraseTooLongException
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Why a [Mnemonic] could not be turned into a saved wallet */
internal sealed interface HotWalletImportError {

    /** The very same wallet is already in the wallets list */
    data object AlreadySaved : HotWalletImportError

    /** The passphrase exceeds [maxByteCount] UTF-8 bytes in NFKD form */
    data class PassphraseTooLong(val maxByteCount: Int) : HotWalletImportError

    data object LimitReached : HotWalletImportError

    data class Unknown(val cause: Throwable? = null) : HotWalletImportError
}

/**
 * Shared "materialize a [Mnemonic] into a saved hot [UserWallet.Hot]" step.
 *
 * Extracted verbatim from the seed-import flow so the recovery-phrase import and the cloud-backup
 * restore converge on the exact same save/sync/analytics sequence: import via the Hot SDK, build the
 * wallet, persist it, kick off the remote sync + assets discovery, and send the onboarding analytics
 * ([AnalyticsParam.WalletCreationType.SeedImport] unless the caller passes another type). Callers own only
 * their progress UI, the navigation callback and the seed-phrase backup state they import with. A caller
 * that must keep the wallet out of the list until something else succeeds runs [materialize] and [save]
 * on its own and reports the creation itself.
 */
@Suppress("LongParameterList")
internal class HotWalletImporter @Inject constructor(
    private val tangemHotSdk: TangemHotSdk,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveUserWalletUseCase: SaveWalletUseCase,
    private val syncWalletWithRemoteUseCase: SyncWalletWithRemoteUseCase,
    private val startAssetsDiscoveryUseCase: StartAssetsDiscoveryUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appsFlyerRepository: AppsFlyerRepository,
    private val isWalletAlreadySavedUseCase: IsWalletAlreadySavedUseCase,
    private val getHotWalletAdditionRestrictionUseCase: GetHotWalletAdditionRestrictionUseCase,
    private val discardUnsavedHotWalletUseCase: DiscardUnsavedHotWalletUseCase,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
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
        creationType: AnalyticsParam.WalletCreationType = AnalyticsParam.WalletCreationType.SeedImport,
    ): Either<HotWalletImportError, UserWalletId> = either {
        val userWallet = materialize(mnemonic, passphrase, name).bind()
        onCancel(
            fa = { save(scope, userWallet, isSeedPhraseBackedUp) },
            onCancel = { discardUnsavedHotWalletUseCase(userWallet.hotWalletId) },
        ).bind()
        sendWalletCreatedEvents(mnemonic, passphrase, creationType)

        userWallet.walletId
    }

    /**
     * Puts the wallet into the Hot SDK storage only; the wallets list does not see it yet. Nothing is written
     * to the SDK for a wallet that is already listed or once the hot wallets limit is reached.
     */
    suspend fun materialize(
        mnemonic: Mnemonic,
        passphrase: CharArray?,
        name: String? = null,
    ): Either<HotWalletImportError, UserWallet.Hot> = either {
        ensureCanBeAdded(mnemonic, passphrase)

        // The SDK writes the keys before it returns, so a cancel at that point would leave them with no id to discard
        val hotWalletId = withContext(NonCancellable) {
            runSuspendCatching { tangemHotSdk.importWallet(mnemonic, passphrase, HotAuth.NoAuth) }
        }.getOrElse { error -> raise(error.toImportError()) }

        onCancel(
            fa = { runSuspendCatching { hotUserWalletBuilderFactory.create(hotWalletId).build(name = name) } },
            onCancel = { discardUnsavedHotWalletUseCase(hotWalletId) },
        ).getOrElse { error ->
            withContext(NonCancellable) { discardUnsavedHotWalletUseCase(hotWalletId) }
            raise(error.toImportError())
        }
    }

    private suspend fun Raise<HotWalletImportError>.ensureCanBeAdded(mnemonic: Mnemonic, passphrase: CharArray?) {
        val isAlreadySaved = isWalletAlreadySavedUseCase(mnemonic = mnemonic, passphrase = passphrase?.let(::String))
            .getOrElse { error ->
                TangemLogger.e("Unable to check whether the wallet is already saved: $error")
                // Without the toggle the import goes on as before and the save still catches a duplicate
                if (!hotWalletFeatureToggles.isMultipleHotWalletsEnabled) return@getOrElse false
                raise(HotWalletImportError.Unknown())
            }
        if (isAlreadySaved) raise(HotWalletImportError.AlreadySaved)

        val restriction = getHotWalletAdditionRestrictionUseCase()
        if (restriction == HotWalletAdditionRestriction.LimitReached) raise(HotWalletImportError.LimitReached)
    }

    private fun Throwable.toImportError(): HotWalletImportError {
        TangemLogger.e("Unable to import the wallet", this)
        return when (this) {
            is PassphraseTooLongException -> HotWalletImportError.PassphraseTooLong(maxByteCount = maxByteCount)
            else -> HotWalletImportError.Unknown(this)
        }
    }

    /**
     * Adds a [materialize]d wallet to the wallets list and starts its remote sync. A failed save deletes the
     * wallet from the Hot SDK storage, unless it got listed anyway.
     */
    suspend fun save(
        scope: CoroutineScope,
        userWallet: UserWallet.Hot,
        isSeedPhraseBackedUp: Boolean,
    ): Either<HotWalletImportError, Unit> = either {
        persist(userWallet, isSeedPhraseBackedUp).bind()
        startRemoteSync(scope, userWallet.walletId)
    }

    private suspend fun persist(
        userWallet: UserWallet.Hot,
        isSeedPhraseBackedUp: Boolean,
    ): Either<HotWalletImportError, Unit> = either {
        // A save cancelled half way leaves the wallet on disk but not yet listed, so the cleanup would take its keys
        val saveResult = withContext(NonCancellable) {
            runSuspendCatching { saveUserWalletUseCase(userWallet.copy(isBackedUp = isSeedPhraseBackedUp)) }
        }
        val error = saveResult.getOrNull()?.leftOrNull()
        val exception = saveResult.exceptionOrNull()
        if (error == null && exception == null) return@either
        TangemLogger.e("Unable to save user wallet: $error", exception)

        val isListedAnyway = withContext(NonCancellable) { discardUnsavedHotWalletUseCase(userWallet.hotWalletId) }
        when (error) {
            // Setting the lock can fail after the wallet got listed; reporting it would make the retry hit AlreadySaved
            null,
            is SaveWalletError.DataError,
            -> if (!isListedAnyway || !hotWalletFeatureToggles.isMultipleHotWalletsEnabled) {
                raise(HotWalletImportError.Unknown(exception))
            }
            is SaveWalletError.WalletAlreadySaved -> raise(HotWalletImportError.AlreadySaved)
            is SaveWalletError.HotWalletsLimitReached -> raise(HotWalletImportError.LimitReached)
        }
    }

    private fun startRemoteSync(scope: CoroutineScope, userWalletId: UserWalletId) {
        scope.launch(dispatchers.main + NonCancellable) {
            val syncResult = syncWalletWithRemoteUseCase(userWalletId)
            if (syncResult == WalletSyncResult.Created) {
                startAssetsDiscoveryUseCase(userWalletId)
            }
        }
    }

    private suspend fun sendWalletCreatedEvents(
        mnemonic: Mnemonic,
        passphrase: CharArray?,
        creationType: AnalyticsParam.WalletCreationType,
    ) {
        analyticsEventHandler.send(
            event = OnboardingAnalyticsEvent.Onboarding.Finished(
                source = AnalyticsParam.ScreensSources.ImportWallet,
            ),
        )
        analyticsEventHandler.send(
            event = OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully(
                source = AnalyticsParam.ScreensSources.ImportWallet,
                creationType = creationType,
                seedPhraseLength = mnemonic.mnemonicComponents.size,
                passPhraseState = if (passphrase?.any { char -> !char.isWhitespace() } == true) {
                    AnalyticsParam.EmptyFull.Full
                } else {
                    AnalyticsParam.EmptyFull.Empty
                },
                referralId = appsFlyerRepository.getConversionData()?.refcode,
            ),
        )
    }
}