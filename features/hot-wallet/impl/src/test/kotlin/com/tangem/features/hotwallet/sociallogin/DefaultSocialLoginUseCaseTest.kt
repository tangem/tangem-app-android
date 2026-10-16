package com.tangem.features.hotwallet.sociallogin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.error.SaveWalletError
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
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
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
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.coroutines.cancellation.CancellationException

/**
 * Covers the orderings it cannot get wrong: the binding before the cloud file, and the wallet reaching the
 * wallets list only after the backend and the cloud are both done with it — deleted from the Hot SDK otherwise.
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
    private val getSocialBackupInfoUseCase: GetSocialBackupInfoUseCase = mockk()
    private val readSocialBackupUseCase: ReadSocialBackupUseCase = mockk(relaxed = true)
    private val mnemonicRepository: MnemonicRepository = mockk()
    private val hotWalletImporter: HotWalletImporter = mockk()
    private val registerRestoredWalletUseCase: RegisterRestoredWalletUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val revokeSocialRecoveryUseCase: RevokeSocialRecoveryUseCase = mockk()

    private val hotWalletId: HotWalletId = mockk()
    private val userWallet = UserWallet.Hot(
        name = "Wallet",
        walletId = WALLET_ID,
        hotWalletId = hotWalletId,
        wallets = emptyList(),
        isBackedUp = false,
    )
    private val savedWallet = userWallet.copy(isSocialBackupCompleted = true)
    private val restoredHotWalletId: HotWalletId = mockk()
    private val restoredWallet = UserWallet.Hot(
        name = "Restored",
        walletId = RESTORED_WALLET_ID,
        hotWalletId = restoredHotWalletId,
        wallets = emptyList(),
        isBackedUp = false,
    )
    private val savedRestoredWallet = restoredWallet.copy(isSocialBackupCompleted = true)

    private lateinit var useCase: DefaultSocialLoginUseCase

    private fun createUseCase() = DefaultSocialLoginUseCase(
        signInWithSocialProviderUseCase = signInWithSocialProviderUseCase,
        signOutFromSocialProviderUseCase = mockk<SignOutFromSocialProviderUseCase>(relaxed = true),
        getSocialRecoveryKeyUseCase = getSocialRecoveryKeyUseCase,
        generateRecoveryKeyUseCase = generateRecoveryKeyUseCase,
        writeSocialBackupUseCase = writeSocialBackupUseCase,
        readSocialBackupUseCase = readSocialBackupUseCase,
        getSocialBackupInfoUseCase = getSocialBackupInfoUseCase,
        registerSocialRecoveryUseCase = registerSocialRecoveryUseCase,
        registerRestoredWalletUseCase = registerRestoredWalletUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        revokeSocialRecoveryUseCase = revokeSocialRecoveryUseCase,
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
            readSocialBackupUseCase,
            getSocialBackupInfoUseCase,
            mnemonicRepository,
            hotWalletImporter,
            registerRestoredWalletUseCase,
            getUserWalletUseCase,
            revokeSocialRecoveryUseCase,
        )

        useCase = createUseCase()

        val builder: HotUserWalletBuilder = mockk {
            coEvery { build() } returns userWallet
        }

        coEvery { signInWithSocialProviderUseCase(PROVIDER) } returns ID_TOKEN.right()
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns SocialLoginError.BindingNotFound.left()
        coEvery { getSocialBackupInfoUseCase() } returns SocialLoginError.BackupNotFound.left()
        coEvery { tangemHotSdk.generateWallet(any(), any()) } returns hotWalletId
        coEvery { tangemHotSdk.delete(any()) } just Runs
        every { hotUserWalletBuilderFactory.create(hotWalletId) } returns builder
        coEvery { saveWalletUseCase(savedWallet) } returns Either.Right(Unit)
        every { generateRecoveryKeyUseCase() } returns RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })
        val privateInfo: SeedPhrasePrivateInfo = mockk {
            every { mnemonic.mnemonicComponents } returns listOf("abandon", "ability")
        }
        coEvery { exportSeedPhraseUseCase(hotWalletId) } returns privateInfo.right()
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns Either.Right(Unit)
        coEvery { revokeSocialRecoveryUseCase(any()) } returns Unit.right()

        every { mnemonicRepository.generateMnemonic(any<String>()) } returns mockk()
        every { getUserWalletUseCase(any()) } returns GetUserWalletError.UserWalletNotFound.left()
        coEvery { hotWalletImporter.materialize(any(), any(), any()) } returns restoredWallet.right()
        coEvery { hotWalletImporter.save(any(), savedRestoredWallet, any()) } returns Unit.right()
        coEvery { registerRestoredWalletUseCase(restoredWallet) } returns Unit.right()
    }

    private fun restoredBackup() = RestoredSocialBackup(
        info = SocialBackupInfo(walletId = RESTORED_WALLET_ID, createdAtMillis = 0L, version = 1),
        secret = SocialBackupSecret(mnemonic = "abandon ability".toCharArray(), isPassphraseRequired = false),
    )

    private fun bindIdentity() {
        coEvery { getSocialRecoveryKeyUseCase(ID_TOKEN) } returns
            RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 3 }).right()
        coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().right()
        coEvery { getSocialBackupInfoUseCase() } returns restoredBackup().info.right()
    }

    @Test
    fun `GIVEN identity has no wallet WHEN signIn THEN the wallet is listed only after the binding and the file`() =
        runTest {
            // Act
            val actual = useCase.signIn(PROVIDER) { CONSENT }

            // Assert
            assertThat(actual).isEqualTo(SocialLoginOutcome.WalletCreated(WALLET_ID).right())
            coVerifyOrder {
                registerSocialRecoveryUseCase(ID_TOKEN, userWallet, any())
                writeSocialBackupUseCase(WALLET_ID, any(), any())
                saveWalletUseCase(savedWallet)
            }
            coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
        }

    @ParameterizedTest
    @MethodSource("provideRegisterErrors")
    fun `GIVEN register fails WHEN signIn THEN the binding is released AND the wallet deleted`(
        error: SocialLoginError,
    ) = runTest {
        // Arrange
        coEvery { registerSocialRecoveryUseCase(any(), any(), any()) } returns error.left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(error.left())
        coVerifyOrder {
            revokeSocialRecoveryUseCase(WALLET_ID)
            tangemHotSdk.delete(hotWalletId)
        }
        coVerify(exactly = 0) {
            writeSocialBackupUseCase(any(), any(), any())
            saveWalletUseCase(any())
        }
    }

    private fun provideRegisterErrors() = listOf(
        SocialLoginError.BindingConflict,
        SocialLoginError.NetworkError,
        SocialLoginError.ServerError(),
    )

    @Test
    fun `GIVEN the cloud write fails AND no retry WHEN signIn THEN the binding is released AND the wallet deleted`() =
        runTest {
            // Arrange
            coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns SocialLoginError.CloudUnavailable.left()

            // Act
            val actual = useCase.signIn(PROVIDER, requestBackupRetry = { false }) { CONSENT }

            // Assert
            assertThat(actual).isEqualTo(SocialLoginError.BackupAbandoned.left())
            coVerifyOrder {
                revokeSocialRecoveryUseCase(WALLET_ID)
                tangemHotSdk.delete(hotWalletId)
            }
            coVerify(exactly = 0) { saveWalletUseCase(any()) }
        }

    @ParameterizedTest
    @MethodSource("provideWriteErrors")
    fun `GIVEN the cloud write fails WHEN signIn THEN the retry is offered`(error: SocialLoginError) = runTest {
        // Arrange
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returns error.left()
        var retries = 0

        // Act
        val actual = useCase.signIn(
            provider = PROVIDER,
            requestBackupRetry = {
                retries++
                false
            },
            requestConsent = { CONSENT },
        )

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.BackupAbandoned.left())
        assertThat(retries).isEqualTo(1)
    }

    private fun provideWriteErrors() = listOf(
        SocialLoginError.NetworkError,
        SocialLoginError.CloudUnavailable,
        SocialLoginError.BackupWriteFailed(),
    )

    @Test
    fun `GIVEN the cloud write fails WHEN retried THEN only the write repeats on the same wallet and key`() = runTest {
        // Arrange
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } returnsMany listOf(
            SocialLoginError.CloudUnavailable.left(),
            Unit.right(),
        )
        var retries = 0

        // Act
        val actual = useCase.signIn(
            provider = PROVIDER,
            requestBackupRetry = {
                retries++
                true
            },
            requestConsent = { CONSENT },
        )

        // Assert
        assertThat(actual).isEqualTo(SocialLoginOutcome.WalletCreated(WALLET_ID).right())
        assertThat(retries).isEqualTo(1)
        coVerify(exactly = 1) {
            tangemHotSdk.generateWallet(any(), any())
            generateRecoveryKeyUseCase()
            registerSocialRecoveryUseCase(any(), any(), any())
            saveWalletUseCase(savedWallet)
        }
        coVerify(exactly = 2) { writeSocialBackupUseCase(WALLET_ID, any(), any()) }
        coVerify(exactly = 0) {
            revokeSocialRecoveryUseCase(any())
            tangemHotSdk.delete(any())
        }
    }

    @Test
    fun `GIVEN the created wallet cannot be listed WHEN signIn THEN it is deleted from the SDK`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(savedWallet) } returns mockk<SaveWalletError>().left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isInstanceOf(Either.Left::class.java)
        coVerify(exactly = 1) { tangemHotSdk.delete(hotWalletId) }
    }

    @Test
    fun `GIVEN the wallet is listed but its save fails later WHEN signIn THEN its keys are kept`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(savedWallet) } returns mockk<SaveWalletError>().left()
        every { getUserWalletUseCase(WALLET_ID) } returns savedWallet.right()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isInstanceOf(Either.Left::class.java)
        coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
    }

    @Test
    fun `GIVEN the sign-in is cancelled while backing up WHEN signIn THEN the unlisted wallet is deleted`() = runTest {
        // Arrange
        coEvery { writeSocialBackupUseCase(any(), any(), any()) } throws CancellationException()

        // Act
        val actual = runCatching { useCase.signIn(PROVIDER) { CONSENT } }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(CancellationException::class.java)
        coVerifyOrder {
            revokeSocialRecoveryUseCase(WALLET_ID)
            tangemHotSdk.delete(hotWalletId)
        }
    }

    @Test
    fun `GIVEN the sign-in is cancelled after the wallet is listed WHEN signIn THEN its keys are kept`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(savedWallet) } throws CancellationException()
        every { getUserWalletUseCase(WALLET_ID) } returns savedWallet.right()

        // Act
        val actual = runCatching { useCase.signIn(PROVIDER) { CONSENT } }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(CancellationException::class.java)
        coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
    }

    @Test
    fun `GIVEN a cancel during the save WHEN signIn THEN the save finishes and the keys are kept`() =
        runTest {
            // Arrange
            val saveGate = CompletableDeferred<Unit>()
            var isListed = false
            coEvery { saveWalletUseCase(savedWallet) } coAnswers {
                saveGate.await()
                isListed = true
                Unit.right()
            }
            every { getUserWalletUseCase(WALLET_ID) } answers {
                if (isListed) savedWallet.right() else GetUserWalletError.UserWalletNotFound.left()
            }
            val signIn = launch { useCase.signIn(PROVIDER) { CONSENT } }
            advanceUntilIdle()

            // Act
            signIn.cancel()
            saveGate.complete(Unit)
            advanceUntilIdle()

            // Assert
            assertThat(isListed).isTrue()
            coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
        }

    @Test
    fun `GIVEN the sign-in is cancelled while registering a restore WHEN signIn THEN the restored wallet is deleted`() =
        runTest {
            // Arrange
            bindIdentity()
            coEvery { registerRestoredWalletUseCase(restoredWallet) } throws CancellationException()

            // Act
            val actual = runCatching { useCase.signIn(PROVIDER) { CONSENT } }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(CancellationException::class.java)
            coVerify(exactly = 1) { tangemHotSdk.delete(restoredHotWalletId) }
        }

    @Test
    fun `GIVEN identity owns a wallet WHEN signIn THEN it is registered before it is listed`() = runTest {
        // Arrange
        bindIdentity()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginOutcome.WalletRestored(RESTORED_WALLET_ID).right())
        coVerifyOrder {
            hotWalletImporter.materialize(any(), null, any())
            registerRestoredWalletUseCase(restoredWallet)
            hotWalletImporter.save(any(), savedRestoredWallet, false)
        }
    }

    @Test
    fun `GIVEN the restored wallet is already on the device WHEN signIn THEN nothing is imported`() = runTest {
        // Arrange
        bindIdentity()
        every { getUserWalletUseCase(RESTORED_WALLET_ID) } returns restoredWallet.right()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.WalletAlreadySaved.left())
        coVerify(exactly = 0) {
            hotWalletImporter.materialize(any(), any(), any())
            registerRestoredWalletUseCase(any())
            tangemHotSdk.delete(any())
        }
    }

    @Test
    fun `GIVEN the registration fails WHEN signIn THEN the restored wallet is deleted from the SDK unlisted`() =
        runTest {
            // Arrange
            bindIdentity()
            coEvery { registerRestoredWalletUseCase(restoredWallet) } returns SocialLoginError.NetworkError.left()

            // Act
            val actual = useCase.signIn(PROVIDER) { CONSENT }

            // Assert
            assertThat(actual).isEqualTo(SocialLoginError.NetworkError.left())
            coVerify(exactly = 1) { tangemHotSdk.delete(restoredHotWalletId) }
            coVerify(exactly = 0) { hotWalletImporter.save(any(), any(), any()) }
        }

    @Test
    fun `GIVEN the restored wallet cannot be listed WHEN signIn THEN it is deleted from the SDK`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { hotWalletImporter.save(any(), savedRestoredWallet, any()) } returns
            HotWalletImportError.Unknown().left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isInstanceOf(Either.Left::class.java)
        coVerify(exactly = 1) { tangemHotSdk.delete(restoredHotWalletId) }
    }

    @Test
    fun `GIVEN the backup cannot be read WHEN signIn THEN nothing is imported`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns SocialLoginError.BackupNotFound.left()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.BackupNotFound.left())
        coVerify(exactly = 0) { hotWalletImporter.materialize(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the backup needs a passphrase WHEN signIn THEN it is refused instead of imported`() = runTest {
        // Arrange
        bindIdentity()
        coEvery { readSocialBackupUseCase(any()) } returns restoredBackup().copy(
            secret = SocialBackupSecret(mnemonic = "abandon ability".toCharArray(), isPassphraseRequired = true),
        ).right()

        // Act
        val actual = useCase.signIn(PROVIDER) { CONSENT }

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.PassphraseNotSupported.left())
        coVerify(exactly = 0) { hotWalletImporter.materialize(any(), any(), any()) }
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
    fun `GIVEN the consent is declined WHEN signIn THEN the backend is not asked for the key`() = runTest {
        // Act
        useCase.signIn(PROVIDER) { null }

        // Assert
        coVerify(exactly = 0) { getSocialRecoveryKeyUseCase(any()) }
    }

    @ParameterizedTest
    @MethodSource("provideMissingBackupErrors")
    fun `GIVEN no readable backup AND a bound identity WHEN signIn THEN consent is asked before the recovery`(
        error: SocialLoginError,
    ) = runTest {
        // Arrange
        bindIdentity()
        coEvery { getSocialBackupInfoUseCase() } returns error.left()
        var wasConsentAsked = false

        // Act
        useCase.signIn(PROVIDER) {
            wasConsentAsked = true
            coVerify(exactly = 0) { getSocialRecoveryKeyUseCase(any()) }
            CONSENT
        }

        // Assert
        assertThat(wasConsentAsked).isTrue()
    }

    private fun provideMissingBackupErrors() = listOf(
        SocialLoginError.BackupNotFound,
        SocialLoginError.CloudUnavailable,
        SocialLoginError.NetworkError,
    )

    @Test
    fun `GIVEN a backup file AND no binding WHEN signIn THEN the wallet is created without asking again`() =
        runTest {
            // Arrange
            coEvery { getSocialBackupInfoUseCase() } returns restoredBackup().info.right()
            var wasConsentAsked = false

            // Act
            val actual = useCase.signIn(PROVIDER) {
                wasConsentAsked = true
                CONSENT
            }

            // Assert
            assertThat(actual).isEqualTo(SocialLoginOutcome.WalletCreated(WALLET_ID).right())
            assertThat(wasConsentAsked).isFalse()
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