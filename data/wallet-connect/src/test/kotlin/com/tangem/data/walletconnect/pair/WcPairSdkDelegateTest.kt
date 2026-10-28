package com.tangem.data.walletconnect.pair

import arrow.core.right
import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.data.walletconnect.store.WalletConnectStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WcPairSdkDelegateTest {

    private val store: WalletConnectStore = mockk()
    private val wcSdk: WcSdk = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(store, wcSdk)
        coEvery { wcSdk.subscribe(any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN pending approvals WHEN onWcSdkInit THEN subscribes to each pairing topic`() = runTest {
        // Arrange
        every { store.pendingApproval } returns flowOf(
            setOf(createPendingApproval(pairingTopic = "first"), createPendingApproval(pairingTopic = "second")),
        )
        val delegate = createDelegate(this)

        // Act
        delegate.onWcSdkInit()
        runCurrent()

        // Assert
        coVerify(exactly = 1) { wcSdk.subscribe("first") }
        coVerify(exactly = 1) { wcSdk.subscribe("second") }
    }

    @Test
    fun `GIVEN no pending approvals WHEN onWcSdkInit THEN does not subscribe`() = runTest {
        // Arrange
        every { store.pendingApproval } returns flowOf(emptySet())
        val delegate = createDelegate(this)

        // Act
        delegate.onWcSdkInit()
        runCurrent()

        // Assert
        coVerify(exactly = 0) { wcSdk.subscribe(any()) }
    }

    // The delegate keeps a settle listener running for its whole life, so it lives on the background scope
    private fun createDelegate(testScope: TestScope) = WcPairSdkDelegate(
        scope = TestAppCoroutineScope(testScope.backgroundScope.coroutineContext),
        store = store,
        wcSdk = wcSdk,
    )

    private fun createPendingApproval(pairingTopic: String) = WcPendingApprovalSessionDTO(
        pairingTopic = pairingTopic,
        session = WcSessionDTO(topic = "", walletId = UserWalletId(stringValue = "011"), url = "https://dapp.example"),
        expiredTime = Long.MAX_VALUE,
    )
}