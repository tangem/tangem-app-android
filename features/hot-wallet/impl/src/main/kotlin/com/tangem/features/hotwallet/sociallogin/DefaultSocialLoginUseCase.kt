package com.tangem.features.hotwallet.sociallogin

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.RestoredSocialBackup
import com.tangem.domain.sociallogin.models.SocialBackupSecret
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.usecase.GenerateRecoveryKeyUseCase
import com.tangem.domain.sociallogin.usecase.GetSocialBackupInfoUseCase
import com.tangem.domain.sociallogin.usecase.GetSocialRecoveryKeyUseCase
import com.tangem.domain.sociallogin.usecase.ReadSocialBackupUseCase
import com.tangem.domain.sociallogin.usecase.RegisterRestoredWalletUseCase
import com.tangem.domain.sociallogin.usecase.RegisterSocialRecoveryUseCase
import com.tangem.domain.sociallogin.usecase.RevokeSocialRecoveryUseCase
import com.tangem.domain.sociallogin.usecase.SignInWithSocialProviderUseCase
import com.tangem.domain.sociallogin.usecase.SignOutFromSocialProviderUseCase
import com.tangem.domain.sociallogin.usecase.WriteSocialBackupUseCase
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.SocialLoginConsent
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.hotwallet.SocialLoginUseCase
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImportError
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImporter
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
@Singleton
internal class DefaultSocialLoginUseCase @Inject constructor(
    private val signInWithSocialProviderUseCase: SignInWithSocialProviderUseCase,
    private val signOutFromSocialProviderUseCase: SignOutFromSocialProviderUseCase,
    private val getSocialRecoveryKeyUseCase: GetSocialRecoveryKeyUseCase,
    private val generateRecoveryKeyUseCase: GenerateRecoveryKeyUseCase,
    private val writeSocialBackupUseCase: WriteSocialBackupUseCase,
    private val readSocialBackupUseCase: ReadSocialBackupUseCase,
    private val getSocialBackupInfoUseCase: GetSocialBackupInfoUseCase,
    private val registerSocialRecoveryUseCase: RegisterSocialRecoveryUseCase,
    private val registerRestoredWalletUseCase: RegisterRestoredWalletUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val revokeSocialRecoveryUseCase: RevokeSocialRecoveryUseCase,
    private val tangemHotSdk: TangemHotSdk,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val hotWalletImporter: HotWalletImporter,
    private val appCoroutineScope: AppCoroutineScope,
) : SocialLoginUseCase {

    private val mutex = Mutex()

    override suspend fun signIn(
        provider: SocialLoginProvider,
        onStageChanged: (SocialLoginStage) -> Unit,
        requestBackupRetry: suspend () -> Boolean,
        requestConsent: suspend () -> SocialLoginConsent?,
    ): Either<SocialLoginError, SocialLoginOutcome> = mutex.withLock {
        onStageChanged(SocialLoginStage.SigningIn)

        val idToken = signInWithSocialProviderUseCase(provider)
            .getOrElse { return@withLock Either.Left(it) }

        resolve(
            idToken = idToken,
            onStageChanged = onStageChanged,
            requestBackupRetry = requestBackupRetry,
            requestConsent = requestConsent,
        )
    }

    override suspend fun signOut(provider: SocialLoginProvider) = mutex.withLock {
        signOutFromSocialProviderUseCase(provider)
    }

    /**
     * The recover call decides the branch: a stored key means restore, "no binding" means create. The consent
     * comes before it and only when the cloud holds no backup: a file means this identity has already been
     * through social login and gave its consent then.
     */
    private suspend fun resolve(
        idToken: SocialProviderIdToken,
        onStageChanged: (SocialLoginStage) -> Unit,
        requestBackupRetry: suspend () -> Boolean,
        requestConsent: suspend () -> SocialLoginConsent?,
    ): Either<SocialLoginError, SocialLoginOutcome> {
        onStageChanged(SocialLoginStage.VerifyingAccount)

        if (getSocialBackupInfoUseCase().isLeft()) {
            requestConsent() ?: return Either.Left(SocialLoginError.ConsentDeclined)
        }

        val recoveryKey = getSocialRecoveryKeyUseCase(idToken).getOrElse { error ->
            return if (error == SocialLoginError.BindingNotFound) {
                create(
                    idToken = idToken,
                    onStageChanged = onStageChanged,
                    requestBackupRetry = requestBackupRetry,
                )
            } else {
                Either.Left(error)
            }
        }

        return restore(recoveryKey, onStageChanged)
    }

    private suspend fun restore(
        recoveryKey: RecoveryKey,
        onStageChanged: (SocialLoginStage) -> Unit,
    ): Either<SocialLoginError, SocialLoginOutcome> {
        try {
            onStageChanged(SocialLoginStage.PreparingWallet)
            val restored = readSocialBackupUseCase(recoveryKey).getOrElse { return Either.Left(it) }

            val userWallet = try {
                // The seed alone builds a different wallet, and a social restore has nowhere to ask for
                // the passphrase. Only another client can write such a file.
                if (restored.secret.isPassphraseRequired) {
                    return Either.Left(SocialLoginError.PassphraseNotSupported)
                }
                // Checked before the import, so the copy already on the device is never touched; the id is
                // safe to take from the header, which survived the AES-GCM check over itself.
                if (isWalletSaved(restored.info.walletId)) return Either.Left(SocialLoginError.WalletAlreadySaved)

                onStageChanged(SocialLoginStage.RestoringWallet)
                materializeWallet(restored).getOrElse { return Either.Left(it) }
            } finally {
                restored.secret.wipe()
            }

            return discardOnCancel(userWallet) {
                registerRestoredWalletUseCase(userWallet).getOrElse { error ->
                    TangemLogger.e("Failed to register the restored social login wallet: $error")
                    discardFromSdk(userWallet)
                    return Either.Left(error)
                }

                saveSocialWallet(userWallet) {
                    hotWalletImporter.save(appCoroutineScope, it, isSeedPhraseBackedUp = false)
                }.getOrElse { return Either.Left(it) }

                Either.Right(SocialLoginOutcome.WalletRestored(userWallet.walletId))
            }
        } finally {
            recoveryKey.wipe()
        }
    }

    /**
     * The wallet stays in the Hot SDK storage only until the backend binding and the cloud file are both in
     * place; any failure before that deletes it, so the wallets list never shows a wallet social login
     * cannot bring back.
     */
    private suspend fun create(
        idToken: SocialProviderIdToken,
        onStageChanged: (SocialLoginStage) -> Unit,
        requestBackupRetry: suspend () -> Boolean,
    ): Either<SocialLoginError, SocialLoginOutcome> {
        onStageChanged(SocialLoginStage.CreatingWallet)

        val userWallet = createHotWallet().getOrElse { return Either.Left(it) }

        return discardOnCancel(userWallet) {
            backUpCreatedWallet(idToken, userWallet, requestBackupRetry).getOrElse { error ->
                discardFromSdk(userWallet)
                return Either.Left(error)
            }

            saveSocialWallet(userWallet) { saveWalletUseCase(it).map { } }.getOrElse { return Either.Left(it) }

            Either.Right(SocialLoginOutcome.WalletCreated(userWallet.walletId))
        }
    }

    private suspend inline fun <T> discardOnCancel(userWallet: UserWallet.Hot, block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        withContext(NonCancellable) {
            if (!isListedWithItsKeys(userWallet)) discardFromSdk(userWallet)
        }
        throw e
    }

    /**
     * A failed write is retried on the same wallet, key and phrase, without binding again: the binding is
     * already there, and only this attempt still holds the key it was made with. Giving up releases it, or
     * the identity would come back to a restore with no file to restore from.
     */
    private suspend fun backUpCreatedWallet(
        idToken: SocialProviderIdToken,
        userWallet: UserWallet.Hot,
        requestBackupRetry: suspend () -> Boolean,
    ): Either<SocialLoginError, Unit> {
        val secret = exportSecret(userWallet).getOrElse { return Either.Left(it) }
        val recoveryKey = generateRecoveryKeyUseCase()
        try {
            // The binding comes first: the folder holds one file per account, so writing before the backend
            // accepts the binding could overwrite another wallet's backup on the way to a binding conflict.
            // A failed or unanswered register may still have created the binding on the backend, and a binding
            // without a file would turn the next sign-in of this identity into a failed restore.
            registerSocialRecoveryUseCase(
                idToken = idToken,
                userWallet = userWallet,
                recoveryKey = recoveryKey,
            ).getOrElse { error ->
                releaseBinding(userWallet.walletId)
                return Either.Left(error)
            }

            while (true) {
                val error = writeSocialBackupUseCase(
                    walletId = userWallet.walletId,
                    secret = secret,
                    recoveryKey = recoveryKey,
                ).leftOrNull() ?: return Either.Right(Unit)

                TangemLogger.e("Failed to write the social backup: $error")
                if (!requestBackupRetry()) {
                    releaseBinding(userWallet.walletId)
                    return Either.Left(SocialLoginError.BackupAbandoned)
                }
            }
        } catch (e: CancellationException) {
            // leaving the screen is giving up too, and the binding must not outlive the attempt
            withContext(NonCancellable) { releaseBinding(userWallet.walletId) }
            throw e
        } finally {
            secret.wipe()
            recoveryKey.wipe()
        }
    }

    /**
     * Adds the wallet to the list with its social recovery marked in the same record, so it never shows up as
     * a wallet without a backup. Failing to add it deletes it from the Hot SDK as well.
     */
    private suspend fun saveSocialWallet(
        userWallet: UserWallet.Hot,
        save: suspend (UserWallet.Hot) -> Either<*, Unit>,
    ): Either<SocialLoginError, Unit> = withContext(NonCancellable) {
        // the record reaches storage before the in-memory list, which isListedWithItsKeys reads
        save(userWallet.copy(isSocialBackupCompleted = true))
    }
        .onLeft { error -> TangemLogger.e("Failed to save the social login wallet: $error") }
        .mapLeft {
            if (!isListedWithItsKeys(userWallet)) discardFromSdk(userWallet)
            SocialLoginError.Unknown()
        }

    // the same seed may have been listed meanwhile under another Hot SDK id
    private fun isListedWithItsKeys(userWallet: UserWallet.Hot): Boolean = runCatching {
        val listed = getUserWalletUseCase(userWallet.walletId).getOrNull() as? UserWallet.Hot
        listed?.hotWalletId == userWallet.hotWalletId
    }.getOrDefault(false)

    /**
     * Retried because a binding left behind cannot be released later: the wallet is about to leave the device,
     * and the identity would keep failing to restore with no file to restore from. Typically called right after
     * a register that got no answer, so the network is likely to be flaky.
     */
    private suspend fun releaseBinding(walletId: UserWalletId) {
        repeat(RELEASE_BINDING_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(RELEASE_BINDING_RETRY_DELAY * attempt)
            revokeSocialRecoveryUseCase(walletId)
                .onRight { return }
                .onLeft { TangemLogger.e("Failed to release the binding of a wallet left without a backup: $it") }
        }
    }

    private suspend fun discardFromSdk(userWallet: UserWallet.Hot) {
        runSuspendCatching { tangemHotSdk.delete(userWallet.hotWalletId) }
            .onFailure { TangemLogger.e("Failed to delete the social login wallet from the Hot SDK", it) }
    }

    private suspend fun createHotWallet(): Either<SocialLoginError, UserWallet.Hot> = Either
        .catch {
            val hotWalletId = tangemHotSdk.generateWallet(HotAuth.NoAuth, mnemonicType = MnemonicType.Words12)
            hotUserWalletBuilderFactory.create(hotWalletId).build()
        }
        .onLeft { TangemLogger.e("Failed to create the social login wallet", it) }
        .mapLeft { SocialLoginError.Unknown(it) }

    /** A social login wallet is always created without a passphrase */
    private suspend fun exportSecret(userWallet: UserWallet.Hot): Either<SocialLoginError, SocialBackupSecret> =
        exportSeedPhraseUseCase(userWallet.hotWalletId)
            .onLeft { TangemLogger.e("Failed to export the seed phrase of the social login wallet", it) }
            .mapLeft { SocialLoginError.BackupWriteFailed(it) }
            .map { privateInfo ->
                SocialBackupSecret(
                    mnemonic = privateInfo.mnemonic.mnemonicComponents.joinToString(separator = " ").toCharArray(),
                    isPassphraseRequired = false,
                )
            }

    // the use case throws instead of returning a Left while the wallets list is still loading
    private fun isWalletSaved(walletId: UserWalletId): Boolean =
        runCatching { getUserWalletUseCase(walletId).isRight() }.getOrDefault(false)

    private suspend fun materializeWallet(restored: RestoredSocialBackup): Either<SocialLoginError, UserWallet.Hot> {
        // the SDK only parses a String, so this copy of the phrase cannot be wiped
        val mnemonic = runCatching { mnemonicRepository.generateMnemonic(String(restored.secret.mnemonic)) }
            .getOrNull()
            ?: return Either.Left(SocialLoginError.InvalidBackupFile)

        return hotWalletImporter.materialize(mnemonic = mnemonic, passphrase = null).mapLeft { error ->
            when (error) {
                is HotWalletImportError.Unknown -> SocialLoginError.Unknown(error.cause)
                // unreachable: the list was checked above, and the import passes no passphrase
                HotWalletImportError.AlreadySaved -> SocialLoginError.WalletAlreadySaved
                is HotWalletImportError.PassphraseTooLong -> SocialLoginError.Unknown()
            }
        }
    }
}

private const val RELEASE_BINDING_ATTEMPTS = 3
private val RELEASE_BINDING_RETRY_DELAY = 1.seconds