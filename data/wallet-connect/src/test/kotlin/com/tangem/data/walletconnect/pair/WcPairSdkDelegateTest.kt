package com.tangem.data.walletconnect.pair

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.data.walletconnect.store.WalletConnectStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.test.core.ProvideTestModels
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN SDK pair fails WHEN pair THEN maps failure to pair error`(model: PairErrorModel) = runTest {
        // Arrange
        coEvery { wcSdk.pair(URI) } returns model.sdkError.left()
        val delegate = createDelegate(this)

        // Act
        val result = delegate.pair(URI)

        // Assert
        assertThat(result).isEqualTo(Either.Left(model.expected))
    }

    data class PairErrorModel(val sdkError: Throwable, val expected: WcPairError)

    private fun provideTestModels() = listOf(
        PairErrorModel(
            sdkError = Throwable("Pairing error: Pairing URI expired: f453"),
            expected = WcPairError.UriExpired("Pairing error: Pairing URI expired: f453"),
        ),
        PairErrorModel(
            sdkError = Throwable("Pairing error: Pairing expired: f453"),
            expected = WcPairError.UriAlreadyUsed("Pairing error: Pairing expired: f453"),
        ),
        PairErrorModel(
            sdkError = Throwable("No proposal or pending session authenticate request for pairing topic: f453"),
            expected = WcPairError.UriAlreadyUsed(
                "No proposal or pending session authenticate request for pairing topic: f453",
            ),
        ),
        PairErrorModel(
            sdkError = IllegalStateException("Malformed WalletConnect URI"),
            expected = WcPairError.PairingFailed("Malformed WalletConnect URI"),
        ),
    )

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

    private companion object {
        const val URI = "wc:f453@2?symKey=ab12&relay-protocol=irn"
    }
}