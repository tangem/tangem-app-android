package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.models.CreateHotWalletError
import com.tangem.domain.wallets.models.HotWalletAdditionRestriction
import com.tangem.domain.wallets.models.WalletSyncResult
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CreateHotWalletUseCaseTest {

    private val tangemHotSdk: TangemHotSdk = mockk()
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory = mockk()
    private val hotUserWalletBuilder: HotUserWalletBuilder = mockk()
    private val saveWalletUseCase: SaveWalletUseCase = mockk()
    private val syncWalletWithRemoteUseCase: SyncWalletWithRemoteUseCase = mockk()
    private val getHotWalletAdditionRestrictionUseCase: GetHotWalletAdditionRestrictionUseCase = mockk()
    private val discardUnsavedHotWalletUseCase: DiscardUnsavedHotWalletUseCase = mockk()

    private val useCase = CreateHotWalletUseCase(
        tangemHotSdk = tangemHotSdk,
        hotUserWalletBuilderFactory = hotUserWalletBuilderFactory,
        saveWalletUseCase = saveWalletUseCase,
        syncWalletWithRemoteUseCase = syncWalletWithRemoteUseCase,
        getHotWalletAdditionRestrictionUseCase = getHotWalletAdditionRestrictionUseCase,
        discardUnsavedHotWalletUseCase = discardUnsavedHotWalletUseCase,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            tangemHotSdk,
            hotUserWalletBuilderFactory,
            hotUserWalletBuilder,
            saveWalletUseCase,
            syncWalletWithRemoteUseCase,
            getHotWalletAdditionRestrictionUseCase,
            discardUnsavedHotWalletUseCase,
        )
        coEvery { getHotWalletAdditionRestrictionUseCase() } returns null
        coEvery { tangemHotSdk.generateWallet(AUTH, MNEMONIC_TYPE) } returns HOT_WALLET_ID
        every { hotUserWalletBuilderFactory.create(HOT_WALLET_ID) } returns hotUserWalletBuilder
        coEvery { hotUserWalletBuilder.build(any()) } returns USER_WALLET
        coEvery { discardUnsavedHotWalletUseCase(any()) } returns false
        coEvery { syncWalletWithRemoteUseCase(any()) } returns WalletSyncResult.Created
    }

    @Test
    fun `GIVEN hot wallets limit reached WHEN invoke THEN returns LimitReached without generating wallet`() = runTest {
        // Arrange
        coEvery { getHotWalletAdditionRestrictionUseCase() } returns HotWalletAdditionRestriction.LimitReached

        // Act
        val actual = useCase(AUTH, MNEMONIC_TYPE)

        // Assert
        assertThat(actual).isEqualTo(CreateHotWalletError.LimitReached.left())
        coVerify(exactly = 0) { tangemHotSdk.generateWallet(any(), any()) }
    }

    @Test
    fun `GIVEN restriction check throws WHEN invoke THEN returns Failed without generating wallet`() = runTest {
        // Arrange
        val cause = IllegalStateException("wallets not loaded")
        coEvery { getHotWalletAdditionRestrictionUseCase() } throws cause

        // Act
        val actual = useCase(AUTH, MNEMONIC_TYPE)

        // Assert
        assertThat(actual).isEqualTo(CreateHotWalletError.Failed(cause).left())
        coVerify(exactly = 0) { tangemHotSdk.generateWallet(any(), any()) }
    }

    @Test
    fun `GIVEN generateWallet throws WHEN invoke THEN returns Failed without discard`() = runTest {
        // Arrange
        val cause = IllegalStateException("generate failed")
        coEvery { tangemHotSdk.generateWallet(AUTH, MNEMONIC_TYPE) } throws cause

        // Act
        val actual = useCase(AUTH, MNEMONIC_TYPE)

        // Assert
        assertThat(actual).isEqualTo(CreateHotWalletError.Failed(cause).left())
        coVerify(exactly = 0) { discardUnsavedHotWalletUseCase(any()) }
    }

    @Test
    fun `GIVEN build throws WHEN invoke THEN discards wallet and returns Failed`() = runTest {
        // Arrange
        val cause = IllegalStateException("build failed")
        coEvery { hotUserWalletBuilder.build(any()) } throws cause

        // Act
        val actual = useCase(AUTH, MNEMONIC_TYPE)

        // Assert
        assertThat(actual).isEqualTo(CreateHotWalletError.Failed(cause).left())
        coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
        coVerify(exactly = 0) { saveWalletUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN save returns HotWalletsLimitReached WHEN invoke THEN discards wallet and returns LimitReached`() =
        runTest {
            // Arrange
            coEvery { saveWalletUseCase(USER_WALLET) } returns SaveWalletError.HotWalletsLimitReached(null).left()

            // Act
            val actual = useCase(AUTH, MNEMONIC_TYPE)

            // Assert
            assertThat(actual).isEqualTo(CreateHotWalletError.LimitReached.left())
            coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
            coVerify(exactly = 0) { syncWalletWithRemoteUseCase(any()) }
        }

    @Test
    fun `GIVEN DataError for unlisted wallet WHEN invoke THEN discards wallet and returns Failed`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(USER_WALLET) } returns SaveWalletError.DataError(null).left()

        // Act
        val actual = useCase(AUTH, MNEMONIC_TYPE)

        // Assert
        assertThat(actual).isEqualTo(CreateHotWalletError.Failed().left())
        coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
        coVerify(exactly = 0) { syncWalletWithRemoteUseCase(any()) }
    }

    @Test
    fun `GIVEN DataError after wallet got listed WHEN invoke THEN returns wallet and launches sync`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(USER_WALLET) } returns SaveWalletError.DataError(null).left()
        coEvery { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) } returns true

        // Act
        val actual = useCase(AUTH, MNEMONIC_TYPE)

        // Assert
        assertThat(actual).isEqualTo(USER_WALLET.right())
        coVerify(exactly = 1) { syncWalletWithRemoteUseCase(userWalletId = USER_WALLET.walletId) }
    }

    @Test
    fun `GIVEN build in progress WHEN coroutine cancelled THEN generated wallet is discarded`() = runTest {
        // Arrange
        coEvery { hotUserWalletBuilder.build(any()) } coAnswers { awaitCancellation() }
        val job = launch { useCase(AUTH, MNEMONIC_TYPE) }
        runCurrent()

        // Act
        job.cancelAndJoin()

        // Assert
        coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
        coVerify(exactly = 0) { saveWalletUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN save in progress WHEN coroutine cancelled THEN wallet is kept and synced but no result returned`() =
        runTest {
            // Arrange
            coEvery { saveWalletUseCase(USER_WALLET) } coAnswers {
                delay(SAVE_DURATION_MS)
                Unit.right()
            }
            var result: Either<CreateHotWalletError, UserWallet.Hot>? = null
            val job = launch { result = useCase(AUTH, MNEMONIC_TYPE) }
            runCurrent()

            // Act
            job.cancel()
            advanceUntilIdle()

            // Assert
            assertThat(result).isNull()
            coVerify(exactly = 1) { saveWalletUseCase(USER_WALLET) }
            coVerify(exactly = 1) { syncWalletWithRemoteUseCase(userWalletId = USER_WALLET.walletId) }
            coVerify(exactly = 0) { discardUnsavedHotWalletUseCase(any()) }
        }

    @Test
    fun `GIVEN save throws for unlisted wallet WHEN invoke THEN discards wallet and returns Failed`() = runTest {
        // Arrange
        val cause = IllegalStateException("secure storage failed")
        coEvery { saveWalletUseCase(USER_WALLET) } throws cause

        // Act
        val actual = useCase(AUTH, MNEMONIC_TYPE)

        // Assert
        assertThat(actual).isEqualTo(CreateHotWalletError.Failed(cause).left())
        coVerify(exactly = 1) { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) }
        coVerify(exactly = 0) { syncWalletWithRemoteUseCase(any()) }
    }

    @Test
    fun `GIVEN save throws after wallet got listed WHEN invoke THEN returns wallet and launches sync`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(USER_WALLET) } throws IllegalStateException("secure storage failed")
        coEvery { discardUnsavedHotWalletUseCase(HOT_WALLET_ID) } returns true

        // Act
        val actual = useCase(AUTH, MNEMONIC_TYPE)

        // Assert
        assertThat(actual).isEqualTo(USER_WALLET.right())
        coVerify(exactly = 1) { syncWalletWithRemoteUseCase(userWalletId = USER_WALLET.walletId) }
    }

    @Test
    fun `GIVEN save succeeds WHEN invoke THEN returns wallet and launches sync`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(USER_WALLET) } returns Unit.right()

        // Act
        val actual = useCase(AUTH, MNEMONIC_TYPE)

        // Assert
        assertThat(actual).isEqualTo(USER_WALLET.right())
        coVerify(exactly = 1) { syncWalletWithRemoteUseCase(userWalletId = USER_WALLET.walletId) }
        coVerify(exactly = 0) { discardUnsavedHotWalletUseCase(any()) }
    }

    private companion object {
        const val SAVE_DURATION_MS = 1_000L
        val AUTH = HotAuth.NoAuth
        val MNEMONIC_TYPE = MnemonicType.Words12
        val HOT_WALLET_ID = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword)
        val USER_WALLET = UserWallet.Hot(
            name = "Hot",
            walletId = UserWalletId("0A0B"),
            hotWalletId = HOT_WALLET_ID,
            wallets = null,
            isBackedUp = false,
        )
    }
}