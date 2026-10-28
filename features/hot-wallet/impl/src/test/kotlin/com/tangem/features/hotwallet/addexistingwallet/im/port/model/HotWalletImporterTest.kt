package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
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
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class HotWalletImporterTest {

    private val tangemHotSdk: TangemHotSdk = mockk()
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory = mockk()
    private val hotUserWalletBuilder: HotUserWalletBuilder = mockk()
    private val saveUserWalletUseCase: SaveWalletUseCase = mockk()
    private val syncWalletWithRemoteUseCase: SyncWalletWithRemoteUseCase = mockk()
    private val startAssetsDiscoveryUseCase: StartAssetsDiscoveryUseCase = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val appsFlyerRepository: AppsFlyerRepository = mockk(relaxed = true)
    private val isWalletAlreadySavedUseCase: IsWalletAlreadySavedUseCase = mockk()
    private val getHotWalletAdditionRestrictionUseCase: GetHotWalletAdditionRestrictionUseCase = mockk()
    private val discardUnsavedHotWalletUseCase: DiscardUnsavedHotWalletUseCase = mockk()
    private val hotWalletFeatureToggles: HotWalletFeatureToggles = mockk()
    private val mnemonic: Mnemonic = mockk()

    private val importer = HotWalletImporter(
        tangemHotSdk = tangemHotSdk,
        hotUserWalletBuilderFactory = hotUserWalletBuilderFactory,
        saveUserWalletUseCase = saveUserWalletUseCase,
        syncWalletWithRemoteUseCase = syncWalletWithRemoteUseCase,
        startAssetsDiscoveryUseCase = startAssetsDiscoveryUseCase,
        analyticsEventHandler = analyticsEventHandler,
        appsFlyerRepository = appsFlyerRepository,
        isWalletAlreadySavedUseCase = isWalletAlreadySavedUseCase,
        getHotWalletAdditionRestrictionUseCase = getHotWalletAdditionRestrictionUseCase,
        discardUnsavedHotWalletUseCase = discardUnsavedHotWalletUseCase,
        hotWalletFeatureToggles = hotWalletFeatureToggles,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            tangemHotSdk,
            hotUserWalletBuilderFactory,
            hotUserWalletBuilder,
            hotWalletFeatureToggles,
            saveUserWalletUseCase,
            syncWalletWithRemoteUseCase,
            isWalletAlreadySavedUseCase,
            getHotWalletAdditionRestrictionUseCase,
            discardUnsavedHotWalletUseCase,
            analyticsEventHandler,
            appsFlyerRepository,
        )
        coEvery { isWalletAlreadySavedUseCase(mnemonic, null) } returns false.right()
        coEvery { getHotWalletAdditionRestrictionUseCase() } returns null
        coEvery { tangemHotSdk.importWallet(mnemonic, null, HotAuth.NoAuth) } returns HOT_WALLET_ID
        every { hotUserWalletBuilderFactory.create(HOT_WALLET_ID) } returns hotUserWalletBuilder
        coEvery { hotUserWalletBuilder.build(any()) } returns USER_WALLET
        coEvery { discardUnsavedHotWalletUseCase(any()) } returns false
        coEvery { syncWalletWithRemoteUseCase(any()) } returns WalletSyncResult.AlreadyExists
        every { hotWalletFeatureToggles.isMultipleHotWalletsEnabled } returns true
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Materialize {

        @Test
        fun `GIVEN wallet already saved WHEN materialize THEN AlreadySaved without sdk import`() = runTest {
            // Arrange
            coEvery { isWalletAlreadySavedUseCase(mnemonic, null) } returns true.right()

            // Act
            val actual = importer.materialize(mnemonic, passphrase = null)

            // Assert
            assertThat(actual).isEqualTo(HotWalletImportError.AlreadySaved.left())
            coVerify(exactly = 0) { tangemHotSdk.importWallet(any(), any(), any()) }
        }

        @Test
        fun `GIVEN hot wallets limit reached WHEN materialize THEN LimitReached without sdk import`() = runTest {
            // Arrange
            coEvery {
                getHotWalletAdditionRestrictionUseCase()
            } returns HotWalletAdditionRestriction.LimitReached

            // Act
            val actual = importer.materialize(mnemonic, passphrase = null)

            // Assert
            assertThat(actual).isEqualTo(HotWalletImportError.LimitReached.left())
            coVerify(exactly = 0) { tangemHotSdk.importWallet(any(), any(), any()) }
        }

        @Test
        fun `GIVEN already saved check fails WHEN materialize THEN Unknown without sdk import`() = runTest {
            // Arrange
            coEvery { isWalletAlreadySavedUseCase(mnemonic, null) } returns SaveWalletError.DataError(null).left()

            // Act
            val actual = importer.materialize(mnemonic, passphrase = null)

            // Assert
            assertThat(actual).isEqualTo(HotWalletImportError.Unknown().left())
            coVerify(exactly = 0) { tangemHotSdk.importWallet(any(), any(), any()) }
        }

        @Test
        fun `GIVEN toggle off AND already saved check fails WHEN materialize THEN import goes on`() = runTest {
            // Arrange
            every { hotWalletFeatureToggles.isMultipleHotWalletsEnabled } returns false
            coEvery { isWalletAlreadySavedUseCase(mnemonic, null) } returns SaveWalletError.DataError(null).left()

            // Act
            val actual = importer.materialize(mnemonic, passphrase = null)

            // Assert
            assertThat(actual).isEqualTo(USER_WALLET.right())
        }

        @Test
        fun `GIVEN sdk rejects passphrase WHEN materialize THEN PassphraseTooLong`() = runTest {
            // Arrange
            coEvery { tangemHotSdk.importWallet(mnemonic, null, HotAuth.NoAuth) } throws
                PassphraseTooLongException(MAX_PASSPHRASE_BYTES)

            // Act
            val actual = importer.materialize(mnemonic, passphrase = null)

            // Assert
            assertThat(actual).isEqualTo(HotWalletImportError.PassphraseTooLong(MAX_PASSPHRASE_BYTES).left())
            coVerify(exactly = 0) { discardUnsavedHotWalletUseCase(any()) }
        }

        @Test
        fun `GIVEN build throws after import WHEN materialize THEN imported wallet discarded AND Unknown`() = runTest {
            // Arrange
            val cause = IllegalStateException("build failed")
            coEvery { hotUserWalletBuilder.build(any()) } throws cause

            // Act
            val actual = importer.materialize(mnemonic, passphrase = null)

            // Assert
            assertThat(actual).isEqualTo(HotWalletImportError.Unknown(cause).left())
            coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
        }

        @Test
        fun `GIVEN build cancelled after import WHEN materialize THEN imported wallet discarded`() = runTest {
            // Arrange
            coEvery { hotUserWalletBuilder.build(any()) } throws CancellationException("left the screen")

            // Act
            val actual = runCatching { importer.materialize(mnemonic, passphrase = null) }

            // Assert
            assertThat(actual.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
            coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
        }

        @Test
        fun `GIVEN new wallet below limit WHEN materialize THEN returns built wallet`() = runTest {
            // Act
            val actual = importer.materialize(mnemonic, passphrase = null, name = WALLET_NAME)

            // Assert
            assertThat(actual).isEqualTo(USER_WALLET.right())
            coVerify(exactly = 1) { hotUserWalletBuilder.build(name = WALLET_NAME) }
            coVerify(exactly = 0) { discardUnsavedHotWalletUseCase(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Save {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN save fails for unlisted wallet WHEN save THEN wallet discarded AND error mapped`(model: SaveErrorModel) = runTest {
            // Arrange
            coEvery { saveUserWalletUseCase(any(), any(), any()) } returns model.saveError.left()

            // Act
            val actual = importer.save(backgroundScope, USER_WALLET, isSeedPhraseBackedUp = true)

            // Assert
            assertThat(actual).isEqualTo(model.expected.left())
            coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
            coVerify(exactly = 0) { syncWalletWithRemoteUseCase(any()) }
        }

        private fun provideTestModels() = listOf(
            SaveErrorModel(
                saveError = SaveWalletError.HotWalletsLimitReached(messageId = null),
                expected = HotWalletImportError.LimitReached,
            ),
            SaveErrorModel(
                saveError = SaveWalletError.WalletAlreadySaved(messageId = 1),
                expected = HotWalletImportError.AlreadySaved,
            ),
            SaveErrorModel(
                saveError = SaveWalletError.DataError(messageId = null),
                expected = HotWalletImportError.Unknown(),
            ),
        )

        @Test
        fun `GIVEN save throws for unlisted wallet WHEN save THEN wallet discarded AND Unknown`() = runTest {
            // Arrange
            val cause = IllegalStateException("save failed")
            coEvery { saveUserWalletUseCase(any(), any(), any()) } throws cause

            // Act
            val actual = importer.save(backgroundScope, USER_WALLET, isSeedPhraseBackedUp = true)

            // Assert
            assertThat(actual).isEqualTo(HotWalletImportError.Unknown(cause).left())
            coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
            coVerify(exactly = 0) { syncWalletWithRemoteUseCase(any()) }
        }

        @Test
        fun `GIVEN DataError after wallet got listed WHEN save THEN success AND remote sync started`() = runTest {
            // Arrange
            coEvery { saveUserWalletUseCase(any(), any(), any()) } returns SaveWalletError.DataError(null).left()
            coEvery { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) } returns true

            // Act
            val actual = importer.save(backgroundScope, USER_WALLET, isSeedPhraseBackedUp = true)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) { syncWalletWithRemoteUseCase(USER_WALLET.walletId) }
        }

        @Test
        fun `GIVEN toggle off AND DataError after wallet got listed WHEN save THEN Unknown`() = runTest {
            // Arrange
            every { hotWalletFeatureToggles.isMultipleHotWalletsEnabled } returns false
            coEvery { saveUserWalletUseCase(any(), any(), any()) } returns SaveWalletError.DataError(null).left()
            coEvery { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) } returns true

            // Act
            val actual = importer.save(backgroundScope, USER_WALLET, isSeedPhraseBackedUp = true)

            // Assert
            assertThat(actual).isEqualTo(HotWalletImportError.Unknown().left())
            coVerify(exactly = 0) { syncWalletWithRemoteUseCase(any()) }
        }

        @Test
        fun `GIVEN save succeeds WHEN save THEN wallet kept AND remote sync started`() = runTest {
            // Arrange
            coEvery { saveUserWalletUseCase(any(), any(), any()) } returns Unit.right()

            // Act
            val actual = importer.save(backgroundScope, USER_WALLET, isSeedPhraseBackedUp = true)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) { saveUserWalletUseCase(USER_WALLET.copy(isBackedUp = true), any(), any()) }
            coVerify(exactly = 1) { syncWalletWithRemoteUseCase(USER_WALLET.walletId) }
            coVerify(exactly = 0) { discardUnsavedHotWalletUseCase(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Import {

        @Test
        fun `GIVEN save cancelled WHEN import THEN imported wallet discarded`() = runTest {
            // Arrange
            coEvery { saveUserWalletUseCase(any(), any(), any()) } throws CancellationException("left the screen")

            // Act
            val actual = runCatching {
                importer.import(backgroundScope, mnemonic, passphrase = null, isSeedPhraseBackedUp = true)
            }

            // Assert
            assertThat(actual.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
            coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
            coVerify(exactly = 0) { syncWalletWithRemoteUseCase(any()) }
        }
    }

    data class SaveErrorModel(val saveError: SaveWalletError, val expected: HotWalletImportError)

    private companion object {
        const val MAX_PASSPHRASE_BYTES = 256
        const val WALLET_NAME = "Imported"
        val HOT_WALLET_ID = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword)
        val USER_WALLET = UserWallet.Hot(
            name = WALLET_NAME,
            walletId = UserWalletId("0A0B"),
            hotWalletId = HOT_WALLET_ID,
            wallets = null,
            isBackedUp = false,
        )
    }
}