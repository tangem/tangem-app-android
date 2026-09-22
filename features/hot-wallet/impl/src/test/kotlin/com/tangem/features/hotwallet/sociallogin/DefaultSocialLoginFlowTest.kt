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
import com.tangem.domain.sociallogin.usecase.RegisterSocialRecoveryUseCase
import com.tangem.domain.sociallogin.usecase.SetSocialBackupCompletedUseCase
import com.tangem.domain.sociallogin.usecase.SignInWithSocialProviderUseCase
import com.tangem.domain.sociallogin.usecase.SignOutFromSocialProviderUseCase
import com.tangem.domain.sociallogin.usecase.WriteSocialBackupUseCase
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.SocialLoginOutcome
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
 * Covers the two orderings the flow cannot get wrong: the binding before the cloud file, and the
 * completion flag only once the wallet is actually restorable.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSocialLoginFlowTest {

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

    /** Recreated per test: the flow keeps a half-finished creation in a field, to be resumed by a retry */
    private lateinit var flow: DefaultSocialLoginFlow

    private fun createFlow() = DefaultSocialLoginFlow(
        signInWithSocialProviderUseCase = signInWithSocialProviderUseCase,
        signOutFromSocialProviderUseCase = mockk<SignOutFromSocialProviderUseCase>(relaxed = true),
        getSocialRecoveryKeyUseCase = getSocialRecoveryKeyUseCase,
        generateRecoveryKeyUseCase = generateRecoveryKeyUseCase,
        writeSocialBackupUseCase = writeSocialBackupUseCase,
        readSocialBackupUseCase = readSocialBackupUseCase,
        registerSocialRecoveryUseCase = registerSocialRecoveryUseCase,
        setSocialBackupCompletedUseCase = setSocialBackupCompletedUseCase,
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
        )

        flow = createFlow()

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
        coEvery { hotWalletImporter.import(any(), any(), any(), any(), any()) } returns RESTORED_WALLET_ID.right()
    }

    /** The secret is wiped as the flow reads it, so every test needs a backup of its own */
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
        val actual = flow.signIn(PROVIDER) {}

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
        val actual = flow.signIn(PROVIDER) {}

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
        val actual = flow.signIn(PROVIDER) {}

        // Assert — NFR-01: bound but with no file in the cloud is not a finished setup
        assertThat(actual).isEqualTo(SocialLoginError.CloudUnavailable.left())
        coVerify(exactly = 0) { setSocialBackupCompletedUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN the write failed WHEN signing in again THEN the creation resumes on the bound key`() = runTest {
        // Arrange — the first attempt registers the binding and then fails to write
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns SocialLoginError.CloudUnavailable.left()
        flow.signIn(PROVIDER) {}

        // the backend now answers with the key it was given, as it would for any bound identity
        val boundKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns boundKey.right()
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)

        // Act
        val actual = flow.signIn(PROVIDER) {}

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
        flow.signIn(PROVIDER) {}

        val boundKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns boundKey.right()
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)

        // Act
        val actual = flow.signIn(PROVIDER) {}

        // Assert — the same wallet is finished, not left stranded behind a file that was never written
        assertThat(actual).isEqualTo(SocialLoginOutcome.WalletCreated(WALLET_ID).right())
        coVerify(exactly = 1) { tangemHotSdk.generateWallet(any(), any()) }
        coVerify(exactly = 0) { readSocialBackupUseCase(any()) }
    }

    @Test
    fun `GIVEN the registration timed out WHEN another identity creates THEN that wallet is left alone`() = runTest {
        // Arrange — the answer was lost, so the binding may exist and the wallet is nobody's to give away
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns SocialLoginError.NetworkError.left()
        flow.signIn(PROVIDER) {}

        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)
        every { generateRecoveryKeyUseCase() } returns RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 9 })

        // Act — another identity, with no binding of its own
        flow.signIn(PROVIDER) {}

        // Assert — a wallet of its own, rather than a second key over a binding that may already hold one
        coVerify(exactly = 2) { tangemHotSdk.generateWallet(any(), any()) }
    }

    @Test
    fun `GIVEN a bound pending creation WHEN another identity creates a wallet THEN the bound one survives`() =
        runTest {
            // Arrange — the first identity binds a wallet and then fails to write
            coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
            coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns SocialLoginError.CloudUnavailable.left()
            flow.signIn(PROVIDER) {}

            // another identity, with no binding of its own, goes through the create branch
            every { generateRecoveryKeyUseCase() } returns RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 9 })
            flow.signIn(PROVIDER) {}

            // the first identity comes back
            val boundKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })
            coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns boundKey.right()
            coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)

            // Act
            val actual = flow.signIn(PROVIDER) {}

            // Assert — its creation is still resumable, not replaced by the second one
            assertThat(actual).isEqualTo(SocialLoginOutcome.WalletCreated(WALLET_ID).right())
            coVerify(exactly = 0) { readSocialBackupUseCase(any()) }
        }

    @Test
    fun `GIVEN a pending creation WHEN another identity signs in THEN the pending wallet is not resumed`() = runTest {
        // Arrange — the first identity registers a binding and then fails to write
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns SocialLoginError.CloudUnavailable.left()
        flow.signIn(PROVIDER) {}

        // another identity, with a wallet and so a key of its own
        val otherKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 9 })
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns otherKey.right()
        coEvery { readSocialBackupUseCase(any()) } returns SocialLoginError.BackupNotFound.left()

        // Act
        val actual = flow.signIn(PROVIDER) {}

        // Assert — the restore branch, not the pending wallet
        assertThat(actual).isEqualTo(SocialLoginError.BackupNotFound.left())
        coVerify(exactly = 1) { readSocialBackupUseCase(otherKey) }
        coVerify(exactly = 0) { setSocialBackupCompletedUseCase(WALLET_ID, true) }
    }

    @Test
    fun `GIVEN identity owns a wallet WHEN signIn THEN it is imported before being marked as backed up`() =
        runTest {
            // Arrange
            bindIdentity()
            coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()

            // Act
            val actual = flow.signIn(PROVIDER) {}

            // Assert
            assertThat(actual).isEqualTo(SocialLoginOutcome.WalletRestored(RESTORED_WALLET_ID).right())
            coVerifyOrder {
                hotWalletImporter.import(any(), any(), any(), any(), any())
                setSocialBackupCompletedUseCase(RESTORED_WALLET_ID, true)
            }
            coVerify(exactly = 0) { registerSocialRecoveryUseCase(any(), any(), any()) }
        }

    @Test
    fun `GIVEN the backup cannot be read WHEN signIn THEN nothing is imported or marked`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns SocialLoginError.InvalidBackupFile.left()

        // Act
        val actual = flow.signIn(PROVIDER) {}

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.InvalidBackupFile.left())
        coVerify(exactly = 0) {
            hotWalletImporter.import(any(), any(), any(), any(), any())
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
        val actual = flow.signIn(PROVIDER) {}

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.PassphraseNotSupported.left())
        coVerify(exactly = 0) {
            hotWalletImporter.import(any(), any(), any(), any(), any())
            setSocialBackupCompletedUseCase(any(), any())
        }
    }

    @Test
    fun `GIVEN the import fails WHEN signIn THEN the wallet is not marked as backed up`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()
        val cause = IllegalStateException("sdk")
        coEvery { hotWalletImporter.import(any(), any(), any(), any(), any()) } returns
            HotWalletImportError.Unknown(cause).left()

        // Act
        val actual = flow.signIn(PROVIDER) {}

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.Unknown(cause).left())
        coVerify(exactly = 0) { setSocialBackupCompletedUseCase(any(), any()) }
    }

    private companion object {
        val PROVIDER = SocialLoginProvider.Google
        val WALLET_ID = UserWalletId(stringValue = "0123456789abcdef")
        val RESTORED_WALLET_ID = UserWalletId(stringValue = "fedcba9876543210")
        val ID_TOKEN = SocialProviderIdToken(value = "id-token", nonce = "nonce")
    }
}