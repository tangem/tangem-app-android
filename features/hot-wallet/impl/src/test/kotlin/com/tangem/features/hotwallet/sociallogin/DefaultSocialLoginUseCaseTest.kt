package com.tangem.features.hotwallet.sociallogin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.RestoredSocialBackup
import com.tangem.domain.sociallogin.models.SocialBackupInfo
import com.tangem.domain.sociallogin.models.SocialBackupSecret
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.usecase.GenerateRecoveryKeyUseCase
import com.tangem.domain.sociallogin.usecase.GetSocialRecoveryKeyUseCase
import com.tangem.domain.sociallogin.usecase.ReadSocialBackupUseCase
import com.tangem.domain.sociallogin.usecase.RegisterRestoredWalletUseCase
import com.tangem.domain.sociallogin.usecase.RegisterSocialRecoveryUseCase
import com.tangem.domain.sociallogin.usecase.SetSocialBackupCompletedUseCase
import com.tangem.domain.sociallogin.usecase.SignInWithSocialProviderUseCase
import com.tangem.domain.sociallogin.usecase.SignOutFromSocialProviderUseCase
import com.tangem.domain.sociallogin.usecase.WriteSocialBackupUseCase
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.SocialLoginConsent
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImportError
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImporter
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SeedPhrasePrivateInfo
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Covers the two orderings it cannot get wrong: the binding before the cloud file, and the
 * completion flag only once the wallet is actually restorable.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSocialLoginUseCaseTest {

    private val signInWithSocialProviderUseCase: SignInWithSocialProviderUseCase = mockk()
    private val getSocialRecoveryKeyUseCase: GetSocialRecoveryKeyUseCase = mockk()
    private val generateRecoveryKeyUseCase: GenerateRecoveryKeyUseCase = mockk()
    private val writeSocialBackupUseCase: WriteSocialBackupUseCase = mockk()
    private val registerSocialRecoveryUseCase: RegisterSocialRecoveryUseCase = mockk()
    private val tangemHotSdk: TangemHotSdk = mockk()
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory = mockk()
    private val saveWalletUseCase: SaveWalletUseCase = mockk()
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase = mockk()
    private val setSocialBackupCompletedUseCase: SetSocialBackupCompletedUseCase = mockk(relaxed = true)
    private val readSocialBackupUseCase: ReadSocialBackupUseCase = mockk(relaxed = true)
    private val mnemonicRepository: MnemonicRepository = mockk()
    private val hotWalletImporter: HotWalletImporter = mockk()
    private val registerRestoredWalletUseCase: RegisterRestoredWalletUseCase = mockk()
    private val deleteWalletUseCase: DeleteWalletUseCase = mockk()

    private lateinit var useCase: DefaultSocialLoginUseCase

    private fun createUseCase() = DefaultSocialLoginUseCase(
        signInWithSocialProviderUseCase = signInWithSocialProviderUseCase,
        signOutFromSocialProviderUseCase = mockk<SignOutFromSocialProviderUseCase>(relaxed = true),
        getSocialRecoveryKeyUseCase = getSocialRecoveryKeyUseCase,
        generateRecoveryKeyUseCase = generateRecoveryKeyUseCase,
        writeSocialBackupUseCase = writeSocialBackupUseCase,
        readSocialBackupUseCase = readSocialBackupUseCase,
        registerSocialRecoveryUseCase = registerSocialRecoveryUseCase,
        setSocialBackupCompletedUseCase = setSocialBackupCompletedUseCase,
        registerRestoredWalletUseCase = registerRestoredWalletUseCase,
        deleteWalletUseCase = deleteWalletUseCase,
        tangemHotSdk = tangemHotSdk,
        hotUserWalletBuilderFactory = hotUserWalletBuilderFactory,
        saveWalletUseCase = saveWalletUseCase,
        exportSeedPhraseUseCase = exportSeedPhraseUseCase,
        mnemonicRepository = mnemonicRepository,
        hotWalletImporter = hotWalletImporter,
        appCoroutineScope = mockk<AppCoroutineScope>(relaxed = true),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            signInWithSocialProviderUseCase,
            getSocialRecoveryKeyUseCase,
            generateRecoveryKeyUseCase,
            writeSocialBackupUseCase,
            registerSocialRecoveryUseCase,
            tangemHotSdk,
            hotUserWalletBuilderFactory,
            saveWalletUseCase,
            exportSeedPhraseUseCase,
            setSocialBackupCompletedUseCase,
            readSocialBackupUseCase,
            mnemonicRepository,
            hotWalletImporter,
            registerRestoredWalletUseCase,
            deleteWalletUseCase,
        )

        useCase = createUseCase()

        val hotWalletId: HotWalletId = mockk()
        val userWallet: UserWallet.Hot = mockk {
            every { walletId } returns WALLET_ID
            every { this@mockk.hotWalletId } returns hotWalletId
        }
        val builder: HotUserWalletBuilder = mockk {
            coEvery { build() } returns userWallet
        }

        coEvery { signInWithSocialProviderUseCase(PROVIDER) } returns ID_TOKEN.right()
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns SocialLoginError.BindingNotFound.left()
        coEvery { tangemHotSdk.generateWallet(any(), any()) } returns hotWalletId
        every { hotUserWalletBuilderFactory.create(hotWalletId) } returns builder
        coEvery { saveWalletUseCase(userWallet) } returns Either.Right(Unit)
        every { generateRecoveryKeyUseCase() } returns RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })
        val privateInfo: SeedPhrasePrivateInfo = mockk {
            every { mnemonic.mnemonicComponents } returns listOf("abandon", "ability")
        }
        coEvery { exportSeedPhraseUseCase(hotWalletId) } returns privateInfo.right()
        coEvery { setSocialBackupCompletedUseCase(any(), any()) } returns Either.Right(Unit)

        every { mnemonicRepository.generateMnemonic(any<String>()) } returns mockk()
        coEvery { hotWalletImporter.import(any(), any(), any(), any(), any(), any()) } returns RESTORED_WALLET_ID.right()
        coEvery { registerRestoredWalletUseCase(RESTORED_WALLET_ID) } returns Unit.right()
        coEvery { deleteWalletUseCase(RESTORED_WALLET_ID) } returns false.right()
    }

    private fun restoredBackup() = RestoredSocialBackup(
        info = SocialBackupInfo(walletId = RESTORED_WALLET_ID, createdAtMillis = 0L, version = 1),
        secret = SocialBackupSecret(mnemonic = "abandon ability".toCharArray(), isPassphraseRequired = false),
    )

    private fun bindIdentity() {
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns
            RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 3 }).right()
    }

    @Test
    fun `GIVEN identity has no wallet WHEN signIn THEN binding is registered before the cloud file`() = runTest {
        // Arrange
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginOutcome.WalletCreated(WALLET_ID).right())
        coVerifyOrder {
            registerSocialRecoveryUseCase(any(), any(), any())
            writeSocialBackupUseCase(any(), any(), any())
        }
    }

    @Test
    fun `GIVEN binding is refused WHEN signIn THEN no backup is written over the cloud folder`() = runTest {
        // Arrange
        val error = SocialLoginError.BindingConflict
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns error.left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(error.left())
        coVerify(exactly = 0) { writeSocialBackupUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the cloud write fails WHEN signIn THEN the wallet is not marked as backed up`() = runTest {
        // Arrange
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns SocialLoginError.CloudUnavailable.left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert — bound but with no file in the cloud is not a finished setup
        assertThat(actual).isEqualTo(SocialLoginError.BackupWriteFailed().left())
        coVerify(exactly = 0) { setSocialBackupCompletedUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN the write failed WHEN signing in again THEN the creation resumes on the bound key`() = runTest {
        // Arrange — the first attempt registers the binding and then fails to write
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns SocialLoginError.CloudUnavailable.left()
        useCase.signIn(PROVIDER) { CONSENT }

        // the backend now answers with the key it was given, as it would for any bound identity
        val boundKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns boundKey.right()
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert — resumed, not restored from a file that was never written
        assertThat(actual).isEqualTo(SocialLoginOutcome.WalletCreated(WALLET_ID).right())
        coVerify(exactly = 1) { registerSocialRecoveryUseCase(any(), any(), any()) }
        coVerify(exactly = 1) { setSocialBackupCompletedUseCase(WALLET_ID, true) }
        coVerify(exactly = 0) { readSocialBackupUseCase(any()) }
    }

    @Test
    fun `GIVEN the registration timed out WHEN signing in again THEN the binding it left is recognised`() = runTest {
        // Arrange — the backend persisted the binding and the answer was lost on the way back
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns SocialLoginError.NetworkError.left()
        useCase.signIn(PROVIDER) { CONSENT }

        val boundKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns boundKey.right()
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert — the same wallet is finished, not left stranded behind a file that was never written
        assertThat(actual).isEqualTo(SocialLoginOutcome.WalletCreated(WALLET_ID).right())
        coVerify(exactly = 1) { tangemHotSdk.generateWallet(any(), any()) }
        coVerify(exactly = 0) { readSocialBackupUseCase(any()) }
    }

    @Test
    fun `GIVEN the registration timed out WHEN another identity creates THEN that wallet is left alone`() = runTest {
        // Arrange — the answer was lost, so the binding may exist and the wallet is nobody's to give away
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns SocialLoginError.NetworkError.left()
        useCase.signIn(PROVIDER) { CONSENT }

        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)
        every { generateRecoveryKeyUseCase() } returns RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 9 })

        // Act — another identity, with no binding of its own
        useCase.signIn(PROVIDER) { CONSENT }

        // Assert — a wallet of its own, rather than a second key over a binding that may already hold one
        coVerify(exactly = 2) { tangemHotSdk.generateWallet(any(), any()) }
    }

    @Test
    fun `GIVEN a bound pending creation WHEN another identity creates a wallet THEN the bound one survives`() =
        runTest {
            // Arrange — the first identity binds a wallet and then fails to write
            coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
            coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns SocialLoginError.CloudUnavailable.left()
            useCase.signIn(PROVIDER) { CONSENT }

            // another identity, with no binding of its own, goes through the create branch
            every { generateRecoveryKeyUseCase() } returns RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 9 })
            useCase.signIn(PROVIDER) { CONSENT }

            // the first identity comes back
            val boundKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })
            coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns boundKey.right()
            coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)

            // Act
            val actual = useCase.signIn(PROVIDER) { CONSENT }

            // Assert — its creation is still resumable, not replaced by the second one
            assertThat(actual).isEqualTo(SocialLoginOutcome.WalletCreated(WALLET_ID).right())
            coVerify(exactly = 0) { readSocialBackupUseCase(any()) }
        }

    @Test
    fun `GIVEN a pending creation WHEN another identity signs in THEN the pending wallet is not resumed`() = runTest {
        // Arrange — the first identity registers a binding and then fails to write
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns SocialLoginError.CloudUnavailable.left()
        useCase.signIn(PROVIDER) { CONSENT }

        // another identity, with a wallet and so a key of its own
        val otherKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 9 })
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns otherKey.right()
        coEvery { readSocialBackupUseCase(any()) } returns SocialLoginError.BackupNotFound.left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert — the restore branch, not the pending wallet
        assertThat(actual).isEqualTo(SocialLoginError.BackupNotFound.left())
        coVerify(exactly = 1) { readSocialBackupUseCase(otherKey) }
        coVerify(exactly = 0) { setSocialBackupCompletedUseCase(WALLET_ID, true) }
    }

    @Test
    fun `GIVEN identity owns a wallet WHEN signIn THEN it is marked as backed up before it is imported`() =
        runTest {
            // Arrange
            bindIdentity()
            coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()

            // Act
            val actual = useCase.signIn(PROVIDER) { CONSENT }

            // Assert
            assertThat(actual).isEqualTo(SocialLoginOutcome.WalletRestored(RESTORED_WALLET_ID, isNewOnDevice = true).right())
            coVerifyOrder {
                setSocialBackupCompletedUseCase(RESTORED_WALLET_ID, true)
                hotWalletImporter.import(any(), any(), any(), any(), any(), any())
            }
            coVerify(exactly = 0) { registerSocialRecoveryUseCase(any(), any(), any()) }
        }

    @Test
    fun `GIVEN identity owns a wallet WHEN signIn THEN it is imported without events and registered`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()

        // Act
        useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        coVerifyOrder {
            hotWalletImporter.import(any(), any(), any(), any(), any(), creationType = null)
            registerRestoredWalletUseCase(RESTORED_WALLET_ID)
        }
    }

    @Test
    fun `GIVEN the registration fails WHEN signIn THEN the imported wallet is discarded`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()
        coEvery { registerRestoredWalletUseCase(RESTORED_WALLET_ID) } returns SocialLoginError.NetworkError.left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.NetworkError.left())
        coVerify(exactly = 1) { deleteWalletUseCase(RESTORED_WALLET_ID) }
        coVerify(exactly = 1) { setSocialBackupCompletedUseCase(RESTORED_WALLET_ID, false) }
    }

    @Test
    fun `GIVEN the wallet was already on the device AND the registration fails WHEN signIn THEN it is kept`() =
        runTest {
            // Arrange
            bindIdentity()
            coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()
            coEvery { hotWalletImporter.import(any(), any(), any(), any(), any(), any()) } returns
                HotWalletImportError.AlreadySaved.left()
            coEvery { registerRestoredWalletUseCase(RESTORED_WALLET_ID) } returns SocialLoginError.ServerError().left()

            // Act
            val actual = useCase.signIn(PROVIDER) { CONSENT }

            // Assert
            assertThat(actual).isEqualTo(SocialLoginError.ServerError().left())
            coVerify(exactly = 0) { deleteWalletUseCase(any()) }
        }

    @Test
    fun `GIVEN the backup cannot be read WHEN signIn THEN nothing is imported or marked`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns SocialLoginError.InvalidBackupFile.left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.InvalidBackupFile.left())
        coVerify(exactly = 0) {
            hotWalletImporter.import(any(), any(), any(), any(), any(), any())
            setSocialBackupCompletedUseCase(any(), any())
        }
    }

    @Test
    fun `GIVEN the backup needs a passphrase WHEN signIn THEN it is refused instead of imported`() = runTest {
        // Arrange — importing without the passphrase would hand back a different wallet, silently
        bindIdentity()
        val secret = SocialBackupSecret(mnemonic = "abandon ability".toCharArray(), isPassphraseRequired = true)
        coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().copy(secret = secret).right()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.PassphraseNotSupported.left())
        coVerify(exactly = 0) {
            hotWalletImporter.import(any(), any(), any(), any(), any(), any())
            setSocialBackupCompletedUseCase(any(), any())
        }
    }

    @Test
    fun `GIVEN the import fails WHEN signIn THEN the flag is taken back`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()
        val cause = IllegalStateException("sdk")
        coEvery { hotWalletImporter.import(any(), any(), any(), any(), any(), any()) } returns
            HotWalletImportError.Unknown(cause).left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.Unknown(cause).left())
        coVerifyOrder {
            setSocialBackupCompletedUseCase(RESTORED_WALLET_ID, true)
            setSocialBackupCompletedUseCase(RESTORED_WALLET_ID, false)
        }
    }

    @Test
    fun `GIVEN the flag cannot be stored WHEN signIn THEN the wallet is not imported`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()
        coEvery { setSocialBackupCompletedUseCase(RESTORED_WALLET_ID, true) } returns
            SocialLoginError.PersistenceFailed().left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.PersistenceFailed().left())
        coVerify(exactly = 0) { hotWalletImporter.import(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the consent is declined WHEN signIn THEN nothing is bound or written`() = runTest {
        // Act
        val actual = useCase.signIn(PROVIDER) { null }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.ConsentDeclined.left())
        coVerify(exactly = 0) {
            registerSocialRecoveryUseCase(any(), any(), any())
            writeSocialBackupUseCase(any(), any(), any())
        }
    }

    @Test
    fun `GIVEN the create branch WHEN signIn THEN consent is asked before the wallet is generated`() = runTest {
        // Arrange
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)
        var wasConsentAsked = false

        // Act
        useCase.signIn(PROVIDER) {
            wasConsentAsked = true
            coVerify(exactly = 0) { tangemHotSdk.generateWallet(any(), any()) }
            CONSENT
        }

        // Assert
        assertThat(wasConsentAsked).isTrue()
    }

    @Test
    fun `GIVEN the create branch WHEN signIn THEN the stages are reported in order`() = runTest {
        // Arrange
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)
        val stages = mutableListOf<SocialLoginStage>()

        // Act
        useCase.signIn(provider = PROVIDER, onStageChanged = { stages += it }, requestConsent = { CONSENT })

        // Assert
        assertThat(stages).containsExactly(
            SocialLoginStage.SigningIn,
            SocialLoginStage.VerifyingAccount,
            SocialLoginStage.CreatingWallet,
        ).inOrder()
    }

    @Test
    fun `GIVEN the restore branch WHEN signIn THEN the download and the import are separate stages`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()
        val stages = mutableListOf<SocialLoginStage>()
        var wasConsentAsked = false

        // Act
        useCase.signIn(
            provider = PROVIDER,
            onStageChanged = { stages += it },
            requestConsent = {
                wasConsentAsked = true
                CONSENT
            },
        )

        // Assert
        assertThat(stages).containsExactly(
            SocialLoginStage.SigningIn,
            SocialLoginStage.VerifyingAccount,
            SocialLoginStage.PreparingWallet,
            SocialLoginStage.RestoringWallet,
        ).inOrder()
        assertThat(wasConsentAsked).isFalse()
    }

    private companion object {
        val PROVIDER = SocialLoginProvider.Google
        val WALLET_ID = UserWalletId(stringValue = "0123456789abcdef")
        val RESTORED_WALLET_ID = UserWalletId(stringValue = "fedcba9876543210")
        val ID_TOKEN = SocialProviderIdToken(value = "id-token", nonce = "nonce")
        val CONSENT = SocialLoginConsent(isEmailSharingAllowed = false)
    }
}