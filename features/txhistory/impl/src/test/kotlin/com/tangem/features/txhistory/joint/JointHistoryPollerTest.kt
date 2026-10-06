package com.tangem.features.txhistory.joint

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class JointHistoryPollerTest {

    private val isResumed = MutableStateFlow(true)
    private var reloads = 0

    @Test
    fun `GIVEN a resumed joint currency WHEN three intervals pass THEN history is reloaded three times`() = runTest {
        // Arrange
        launchPoller(currency = jointCurrency)

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS * 3)
        runCurrent()

        // Assert
        assertThat(reloads).isEqualTo(3)
    }

    @Test
    fun `GIVEN a resumed joint currency WHEN less than an interval passes THEN history is not reloaded`() = runTest {
        // Arrange
        launchPoller(currency = jointCurrency)

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS - 1)
        runCurrent()

        // Assert
        assertThat(reloads).isEqualTo(0)
    }

    @Test
    fun `GIVEN a joint currency WHEN paused THEN history is not reloaded`() = runTest {
        // Arrange
        isResumed.value = false
        launchPoller(currency = jointCurrency)

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS * 3)
        runCurrent()

        // Assert
        assertThat(reloads).isEqualTo(0)
    }

    @Test
    fun `GIVEN a polling joint currency WHEN paused and resumed THEN the interval restarts`() = runTest {
        // Arrange
        launchPoller(currency = jointCurrency)
        advanceTimeBy(POLL_INTERVAL_MILLIS - 1)
        isResumed.value = false
        runCurrent()

        // Act
        isResumed.value = true
        advanceTimeBy(POLL_INTERVAL_MILLIS - 1)
        runCurrent()

        // Assert
        assertThat(reloads).isEqualTo(0)
    }

    @Test
    fun `GIVEN a non-joint currency WHEN resumed THEN history is not reloaded`() = runTest {
        // Arrange
        launchPoller(currency = MockCryptoCurrencyFactory().ethereum)

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS * 3)
        runCurrent()

        // Assert
        assertThat(reloads).isEqualTo(0)
    }

    private fun TestScope.launchPoller(currency: CryptoCurrency) {
        reloads = 0
        JointHistoryPoller.launchIn(
            scope = backgroundScope,
            currency = currency,
            isResumed = isResumed,
            reload = { reloads++ },
        )
        runCurrent()
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 10_000L

        val jointCurrency: CryptoCurrency by lazy {
            val ethereum = MockCryptoCurrencyFactory().ethereum
            val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0")
            ethereum.copy(
                network = ethereum.network.copy(
                    id = ethereum.network.id.copy(derivationPath = derivationPath),
                    derivationPath = derivationPath,
                ),
            )
        }
    }
}