package com.tangem.grow.datasource.polymarket.socket

import com.google.common.truth.Truth.assertThat
import com.tangem.core.socket.ClosePolicy
import com.tangem.core.socket.SocketClose
import com.tangem.core.socket.SocketCloseCode
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketPriceClosePolicyTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN close code WHEN decided THEN bad credentials and violations stop and the rest retries`(model: Model) {
        // Act
        val actual = PolymarketPriceClosePolicy().decide(SocketClose(code = model.code, reason = ""))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        Model(code = 4001, expected = ClosePolicy.Decision.Stop),
        Model(code = 4008, expected = ClosePolicy.Decision.Stop),
        Model(code = 4002, expected = ClosePolicy.Decision.Retry),
        Model(code = 1008, expected = ClosePolicy.Decision.Retry),
        Model(code = SocketCloseCode.NORMAL, expected = ClosePolicy.Decision.Retry),
        Model(code = SocketCloseCode.ABNORMAL, expected = ClosePolicy.Decision.Retry),
    )

    data class Model(val code: Int, val expected: ClosePolicy.Decision)

    @Test
    fun `GIVEN server draining WHEN decided repeatedly THEN each retry waits up to ten seconds`() {
        // Arrange
        val policy = PolymarketPriceClosePolicy(random = Random(seed = 7))

        // Act
        val delays = List(size = 200) {
            (policy.decide(SocketClose(code = 4003, reason = "")) as ClosePolicy.Decision.RetryAfter).delay
        }

        // Assert
        assertThat(delays.all { it >= Duration.ZERO && it <= 10.seconds }).isTrue()
        assertThat(delays.distinct().size).isGreaterThan(1)
    }
}