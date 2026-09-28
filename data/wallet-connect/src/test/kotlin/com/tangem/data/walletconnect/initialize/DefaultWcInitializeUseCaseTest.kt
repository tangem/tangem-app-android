package com.tangem.data.walletconnect.initialize

import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.data.walletconnect.store.WalletConnectStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.featuretoggle.WalletConnectFeatureToggles
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultWcInitializeUseCaseTest {

    private val wcSdk: WcSdk = mockk(relaxed = true)
    private val store: WalletConnectStore = mockk()
    private val featureToggles: WalletConnectFeatureToggles = mockk()

    private val useCase = DefaultWcInitializeUseCase(
        wcSdk = wcSdk,
        store = store,
        featureToggles = featureToggles,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(wcSdk, store, featureToggles)
        every { featureToggles.isOnDemandInitEnabled } returns true
    }

    @Test
    fun `GIVEN no saved sessions WHEN initializeOnAppStart THEN sdk stays down`() = runTest {
        // Arrange
        coEvery { store.sessions } returns flowOf(emptySet())
        coEvery { store.pendingApproval } returns flowOf(emptySet())

        // Act
        useCase.initializeOnAppStart()

        // Assert
        coVerify(exactly = 0) { wcSdk.ensureInitialized() }
    }

    @Test
    fun `GIVEN saved session WHEN initializeOnAppStart THEN sdk is initialized`() = runTest {
        // Arrange
        coEvery { store.sessions } returns flowOf(setOf(createSession()))
        coEvery { store.pendingApproval } returns flowOf(emptySet())

        // Act
        useCase.initializeOnAppStart()

        // Assert
        coVerify(exactly = 1) { wcSdk.ensureInitialized() }
    }

    @Test
    fun `GIVEN pending approval WHEN initializeOnAppStart THEN sdk is initialized`() = runTest {
        // Arrange
        coEvery { store.sessions } returns flowOf(emptySet())
        coEvery { store.pendingApproval } returns flowOf(setOf(mockk<WcPendingApprovalSessionDTO>()))

        // Act
        useCase.initializeOnAppStart()

        // Assert
        coVerify(exactly = 1) { wcSdk.ensureInitialized() }
    }

    @Test
    fun `GIVEN toggle disabled WHEN initializeOnAppStart THEN sdk starts without reading sessions`() = runTest {
        // Arrange
        every { featureToggles.isOnDemandInitEnabled } returns false

        // Act
        useCase.initializeOnAppStart()

        // Assert
        coVerify(exactly = 1) { wcSdk.ensureInitialized() }
        coVerify(exactly = 0) { store.sessions }
    }

    @Test
    fun `GIVEN any state WHEN ensureInitialized THEN delegates to sdk`() = runTest {
        // Act
        useCase.ensureInitialized()

        // Assert
        coVerify(exactly = 1) { wcSdk.ensureInitialized() }
    }

    private fun createSession() = WcSessionDTO(
        topic = "topic",
        walletId = UserWalletId(stringValue = "011"),
        url = "https://dapp.example",
    )
}