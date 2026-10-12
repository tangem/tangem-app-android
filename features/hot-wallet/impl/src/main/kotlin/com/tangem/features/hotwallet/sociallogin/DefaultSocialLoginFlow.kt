package com.tangem.features.hotwallet.sociallogin

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.recover
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.RestoredSocialBackup
import com.tangem.domain.sociallogin.models.SocialBackupSecret
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.usecase.GenerateRecoveryKeyUseCase
import com.tangem.domain.sociallogin.usecase.GetSocialRecoveryKeyUseCase
import com.tangem.domain.sociallogin.usecase.ReadSocialBackupUseCase
import com.tangem.domain.sociallogin.usecase.RegisterSocialRecoveryUseCase
import com.tangem.domain.sociallogin.usecase.SetSocialBackupCompletedUseCase
import com.tangem.domain.sociallogin.usecase.SignInWithSocialProviderUseCase
import com.tangem.domain.sociallogin.usecase.SignOutFromSocialProviderUseCase
import com.tangem.domain.sociallogin.usecase.WriteSocialBackupUseCase
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.SocialLoginFlow
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImportError
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImporter
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ordering follows NFR-01: social recovery counts as set up only once both the backend binding and the
 * cloud file are in place, so the completion flag is written last and a failure in between leaves an
 * ordinary hot wallet behind.
 */
@Suppress("LongParameterList")
@Singleton
internal class DefaultSocialLoginFlow @Inject constructor(
    private val signInWithSocialProviderUseCase: SignInWithSocialProviderUseCase,
    private val signOutFromSocialProviderUseCase: SignOutFromSocialProviderUseCase,
    private val getSocialRecoveryKeyUseCase: GetSocialRecoveryKeyUseCase,
    private val generateRecoveryKeyUseCase: GenerateRecoveryKeyUseCase,
    private val writeSocialBackupUseCase: WriteSocialBackupUseCase,
    private val readSocialBackupUseCase: ReadSocialBackupUseCase,
    private val registerSocialRecoveryUseCase: RegisterSocialRecoveryUseCase,
    private val setSocialBackupCompletedUseCase: SetSocialBackupCompletedUseCase,
    private val tangemHotSdk: TangemHotSdk,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val hotWalletImporter: HotWalletImporter,
    private val appCoroutineScope: AppCoroutineScope,
) : SocialLoginFlow {

    /** Serializes the whole flow, so a double tap cannot run two creations against the same identity */
    private val mutex = Mutex()

    /** Wallets created and saved but not backed up yet, kept so a retry resumes one (see [PendingCreate]) */
    private val pendingCreates = mutableListOf<PendingCreate>()

    override suspend fun signIn(
        provider: SocialLoginProvider,
        onStageChanged: (SocialLoginStage) -> Unit,
    ): Either<SocialLoginError, SocialLoginOutcome> = mutex.withLock {
        onStageChanged(SocialLoginStage.SigningIn)

        val idToken = signInWithSocialProviderUseCase(provider)
            .getOrElse { return@withLock Either.Left(it) }

        resolve(idToken, onStageChanged)
    }

    /** Same lock as [signIn]: clearing the provider mid-flow would strand it on a gone authorization */
    override suspend fun signOut(provider: SocialLoginProvider) = mutex.withLock {
        signOutFromSocialProviderUseCase(provider)
    }

    /** The recover call decides the branch: a stored key means restore, "no binding" means create */
    private suspend fun resolve(
        idToken: SocialProviderIdToken,
        onStageChanged: (SocialLoginStage) -> Unit,
    ): Either<SocialLoginError, SocialLoginOutcome> {
        val recoveryKey = getSocialRecoveryKeyUseCase(idToken).getOrElse { error ->
            return if (error == SocialLoginError.BindingNotFound) {
                onStageChanged(SocialLoginStage.CreatingWallet)
                create(idToken)
            } else {
                Either.Left(error)
            }
        }

        // A binding holding a key some creation was registered with is that creation's own — the file was
        // never written, so there is nothing to restore. Any other key belongs to another wallet.
        pendingCreates.firstOrNull { it.wasRegisteredWith(recoveryKey) }?.let { pending ->
            onStageChanged(SocialLoginStage.CreatingWallet)
            return finishCreate(pending, recoveryKey)
        }

        onStageChanged(SocialLoginStage.RestoringWallet)
        return restore(recoveryKey)
    }

    private suspend fun restore(recoveryKey: RecoveryKey): Either<SocialLoginError, SocialLoginOutcome> {
        try {
            val restored = readSocialBackupUseCase(recoveryKey).getOrElse { return Either.Left(it) }

            val userWalletId = try {
                // The seed alone builds a different wallet, and a social restore has nowhere to ask for
                // the passphrase. Only another client can write such a file.
                if (restored.secret.isPassphraseRequired) {
                    return Either.Left(SocialLoginError.PassphraseNotSupported)
                }
                importWallet(restored).getOrElse { return Either.Left(it) }
            } finally {
                restored.secret.wipe()
            }

            setSocialBackupCompletedUseCase(walletId = userWalletId, isCompleted = true)
                .getOrElse { return Either.Left(it) }

            return Either.Right(SocialLoginOutcome.WalletRestored(userWalletId))
        } finally {
            recoveryKey.wipe()
        }
    }

    private suspend fun create(idToken: SocialProviderIdToken): Either<SocialLoginError, SocialLoginOutcome> {
        // Taken over only if no registration was ever attempted on it. An attempt that failed still may
        // have been persisted, and binding such a wallet to a second identity would either be refused or
        // leave the backend holding the first key while this attempt encrypts the file with the second.
        val pending = pendingCreates.firstOrNull { !it.hasRegistrationAttempt }
            ?: PendingCreate(userWallet = createHotWallet().getOrElse { return Either.Left(it) })
                .also { pendingCreates += it }

        // A key per attempt, because an attempt can be a different Google account: the previous key
        // belongs to a file in that account's folder.
        val recoveryKey = generateRecoveryKeyUseCase()

        // Remembered before the call and kept whatever it answers: a timeout says nothing about whether
        // the backend persisted the binding, and the key is the only way to recognise it on a retry.
        pending.rememberRegistration(recoveryKey)

        // The binding comes first: the folder holds one file per account, so writing before the backend
        // accepts the binding could overwrite another wallet's backup on the way to an FR-14 conflict.
        registerSocialRecoveryUseCase(
            idToken = idToken,
            walletId = pending.userWallet.walletId,
            recoveryKey = recoveryKey,
        ).getOrElse { error ->
            recoveryKey.wipe()
            return Either.Left(error)
        }

        return finishCreate(pending, recoveryKey)
    }

    /**
     * Everything after the binding, and the only part a retry can be left with — hence its own method.
     * [recoveryKey] is wiped here whichever way this ends.
     */
    private suspend fun finishCreate(
        pending: PendingCreate,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, SocialLoginOutcome> {
        val userWallet = pending.userWallet
        try {
            val secret = exportSecret(userWallet).getOrElse { return Either.Left(it) }
            try {
                writeSocialBackupUseCase(
                    walletId = userWallet.walletId,
                    secret = secret,
                    recoveryKey = recoveryKey,
                ).getOrElse { return Either.Left(it) }
            } finally {
                secret.wipe()
            }
        } finally {
            recoveryKey.wipe()
        }

        setSocialBackupCompletedUseCase(walletId = userWallet.walletId, isCompleted = true)
            .getOrElse { return Either.Left(it) }

        pending.wipe()
        pendingCreates -= pending

        return Either.Right(SocialLoginOutcome.WalletCreated(userWallet.walletId))
    }

    private suspend fun createHotWallet(): Either<SocialLoginError, UserWallet.Hot> = Either
        .catch {
            val hotWalletId = tangemHotSdk.generateWallet(HotAuth.NoAuth, mnemonicType = MnemonicType.Words12)
            hotUserWalletBuilderFactory.create(hotWalletId).build()
        }
        .onLeft { TangemLogger.e("Failed to create the social login wallet", it) }
        .mapLeft { SocialLoginError.Unknown(it) }
        .flatMap { userWallet ->
            saveWalletUseCase(userWallet)
                .mapLeft { error ->
                    TangemLogger.e("Failed to save the social login wallet: $error")
                    SocialLoginError.Unknown()
                }
                .map { userWallet }
        }

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

    private suspend fun importWallet(restored: RestoredSocialBackup): Either<SocialLoginError, UserWalletId> {
        // the SDK only parses a String, so this copy of the phrase cannot be wiped
        val mnemonic = runCatching { mnemonicRepository.generateMnemonic(String(restored.secret.mnemonic)) }
            .getOrNull()
            ?: return Either.Left(SocialLoginError.InvalidBackupFile)

        return hotWalletImporter.import(
            scope = appCoroutineScope,
            mnemonic = mnemonic,
            passphrase = null,
            isSeedPhraseBackedUp = false,
        ).recover { error ->
            when (error) {
                // already on this device — the state the restore was after; the id is safe to take from
                // the header, which survived the AES-GCM check over itself
                HotWalletImportError.AlreadySaved -> restored.info.walletId
                is HotWalletImportError.Unknown -> raise(SocialLoginError.Unknown(error.cause))
                // unreachable: the import above passes no passphrase
                is HotWalletImportError.PassphraseTooLong -> raise(SocialLoginError.Unknown())
            }
        }
    }

    /**
     * A creation that got as far as saving the wallet, so a retry carries on with it instead of leaving a
     * wallet behind per attempt.
     *
     * Holds every key its registration was attempted with, not just the last: an attempt that timed out
     * may have been persisted anyway, and its key is then the only thing that can recognise the binding.
     * That is also why one attempt is enough to stop the wallet being offered to another identity. The
     * keys are copies — the one an attempt runs on is wiped when the attempt ends.
     */
    private class PendingCreate(val userWallet: UserWallet.Hot) {

        private val registrationKeys = mutableListOf<RecoveryKey>()

        val hasRegistrationAttempt: Boolean get() = registrationKeys.isNotEmpty()

        fun rememberRegistration(recoveryKey: RecoveryKey) {
            registrationKeys += RecoveryKey(recoveryKey.value.copyOf())
        }

        fun wasRegisteredWith(recoveryKey: RecoveryKey) = registrationKeys.any { it == recoveryKey }

        fun wipe() = registrationKeys.forEach { it.wipe() }
    }
}