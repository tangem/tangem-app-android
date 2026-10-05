package com.tangem.domain.polymarket

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketCredentialsLockTest {

    private val lock = PolymarketCredentialsLock()

    @Test
    fun `GIVEN a request in flight for a wallet WHEN credentials are written for it THEN the write waits`() =
        runTest {
            // Arrange
            val events = mutableListOf<String>()
            val response = CompletableDeferred<Unit>()
            launch {
                lock.withLock(WALLET) {
                    response.await()
                    events += "request rejected, credentials cleared"
                }
            }
            runCurrent()

            // Act
            launch { lock.withLock(WALLET) { events += "new credentials stored" } }
            runCurrent()
            val eventsBeforeResponse = events.toList()
            response.complete(Unit)
            runCurrent()

            // Assert
            assertThat(eventsBeforeResponse).isEmpty()
            assertThat(events)
                .containsExactly("request rejected, credentials cleared", "new credentials stored")
                .inOrder()
        }

    @Test
    fun `GIVEN a request in flight for one wallet WHEN another wallet uses its credentials THEN it is not blocked`() =
        runTest {
            // Arrange
            val response = CompletableDeferred<Unit>()
            launch { lock.withLock(WALLET) { response.await() } }
            runCurrent()

            // Act
            val actual = lock.withLock(OTHER_WALLET) { "read" }

            // Assert
            assertThat(actual).isEqualTo("read")
            response.complete(Unit)
        }

    private companion object {
        val WALLET = UserWalletId("011")
        val OTHER_WALLET = UserWalletId("022")
    }
}