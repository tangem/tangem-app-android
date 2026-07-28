package com.tangem.features.hotwallet.accesscoderequest

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.assetsdiscovery.usecase.StartAssetsDiscoveryUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository.AttemptId
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository.Attempts
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester.AttemptRequest
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class HotAccessCodeRequestModelTest {

    private val hotAccessCodeAttemptsRepository: HotWalletAccessCodeAttemptsRepository = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val deleteWalletUseCase: DeleteWalletUseCase = mockk(relaxed = true)
    private val canUseBiometryUseCase: CanUseBiometryUseCase = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val startAssetsDiscoveryUseCase: StartAssetsDiscoveryUseCase = mockk(relaxed = true)

    private val hotWalletIdA: HotWalletId = mockk()
    private val hotWalletIdB: HotWalletId = mockk()
    private val walletIdA = UserWalletId("A")
    private val walletIdB = UserWalletId("B")

    private val userWalletA: UserWallet.Hot = mockk {
        every { hotWalletId } returns hotWalletIdA
        every { walletId } returns walletIdA
    }
    private val userWalletB: UserWallet.Hot = mockk {
        every { hotWalletId } returns hotWalletIdB
        every { walletId } returns walletIdB
    }

    private val requestA = AttemptRequest(hotWalletId = hotWalletIdA, authMode = true, hasBiometry = false)
    private val requestB = AttemptRequest(hotWalletId = hotWalletIdB, authMode = true, hasBiometry = false)

    private val attemptIdA = AttemptId(hotWalletId = hotWalletIdA, auth = true)
    private val attemptIdB = AttemptId(hotWalletId = hotWalletIdB, auth = true)

    @BeforeEach
    fun setUp() {
        clearMocks(hotAccessCodeAttemptsRepository, deleteWalletUseCase, answers = false)
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWalletA, userWalletB)
        every { hotAccessCodeAttemptsRepository.getAttempts(any()) } returns emptyFlow()
    }

    @Test
    fun `GIVEN request A replaced by B WHEN late wrong code of A THEN attempt attributed to A not B`() = runTest {
        // Arrange
        val model = createModel(this)
        model.show(requestA)
        advanceUntilIdle()
        model.show(requestB) // B now owns the dialog
        advanceUntilIdle()

        // Act
        model.wrongAccessCode(requestA) // late callback belonging to A
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { hotAccessCodeAttemptsRepository.incrementAttempts(attemptIdA) }
        coVerify(exactly = 0) { hotAccessCodeAttemptsRepository.incrementAttempts(attemptIdB) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN request A replaced by B WHEN late success of A THEN reset attributed to A not B`() = runTest {
        // Arrange
        val model = createModel(this)
        model.show(requestA)
        advanceUntilIdle()
        model.show(requestB)
        advanceUntilIdle()

        // Act
        model.successfulAuthentication(requestA) // late callback belonging to A
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { hotAccessCodeAttemptsRepository.resetAttempts(hotWalletIdA) }
        coVerify(exactly = 0) { hotAccessCodeAttemptsRepository.resetAttempts(hotWalletIdB) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN request A owns the dialog WHEN wrong code of A THEN attempt incremented for A`() = runTest {
        // Arrange
        val model = createModel(this)
        model.show(requestA)
        advanceUntilIdle()

        // Act
        model.wrongAccessCode(requestA)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { hotAccessCodeAttemptsRepository.incrementAttempts(attemptIdA) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN B reaches deletion threshold WHEN B owns the dialog THEN only B is deleted`() = runTest {
        // Arrange
        every { hotAccessCodeAttemptsRepository.getAttempts(attemptIdB) } returns flowOf(Attempts.Deletion)
        val model = createModel(this)

        // Act
        model.show(requestB)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { deleteWalletUseCase(walletIdB) }
        coVerify(exactly = 0) { deleteWalletUseCase(walletIdA) }

        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): HotAccessCodeRequestModel {
        return HotAccessCodeRequestModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            hotAccessCodeAttemptsRepository = hotAccessCodeAttemptsRepository,
            userWalletsListRepository = userWalletsListRepository,
            deleteWalletUseCase = deleteWalletUseCase,
            canUseBiometryUseCase = canUseBiometryUseCase,
            analyticsEventHandler = analyticsEventHandler,
            startAssetsDiscoveryUseCase = startAssetsDiscoveryUseCase,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}