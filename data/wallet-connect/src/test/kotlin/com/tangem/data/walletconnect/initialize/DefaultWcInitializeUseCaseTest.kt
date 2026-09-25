package com.tangem.data.walletconnect.initialize

import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.data.walletconnect.store.WalletConnectStore
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultWcInitializeUseCaseTest {

    private val wcSdk: WcSdk = mockk(relaxed = true)
    private val store: WalletConnectStore = mockk()

    private val useCase = DefaultWcInitializeUseCase(
        wcSdk = wcSdk,
        store = store,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(wcSdk, store)
    }

    @Test
    fun `GIVEN no saved sessions WHEN initializeIfHasSessions THEN sdk stays down`() = runTest {
        // Arrange
        coEvery { store.sessions } returns flowOf(emptySet())

        // Act
        useCase.initializeIfHasSessions()

        // Assert
        coVerify(exactly = 0) { wcSdk.ensureInitialized() }
    }

    @Test
    fun `GIVEN saved session WHEN initializeIfHasSessions THEN sdk is initialized`() = runTest {
        // Arrange
        coEvery { store.sessions } returns flowOf(setOf(createSession()))

        // Act
        useCase.initializeIfHasSessions()

        // Assert
        coVerify(exactly = 1) { wcSdk.ensureInitialized() }
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