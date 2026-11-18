package com.tangem.domain.polymarket.sessionkey

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class PolymarketPendingAuthorizationTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InFlight {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the batch deadlines WHEN isInFlight THEN it is in flight while either batch may execute`(
            model: DeadlineModel,
        ) {
            // Arrange
            val pending = pendingWith(model)

            // Act
            val actual = pending.isInFlight(NOW_MILLIS)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            DeadlineModel(revocationSecondsLeft = 100, authorizationSecondsLeft = 700, expected = true),
            DeadlineModel(revocationSecondsLeft = -1, authorizationSecondsLeft = 10, expected = true),
            DeadlineModel(revocationSecondsLeft = 10, authorizationSecondsLeft = -1, expected = true),
            DeadlineModel(revocationSecondsLeft = 0, authorizationSecondsLeft = 0, expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MayStillLand {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the batch deadlines WHEN mayStillLand THEN only a batch within its send margin may still land`(
            model: DeadlineModel,
        ) {
            // Arrange
            val pending = pendingWith(model)

            // Act
            val actual = pending.mayStillLand(NOW_MILLIS)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            DeadlineModel(revocationSecondsLeft = 30, authorizationSecondsLeft = 630, expected = true),
            DeadlineModel(revocationSecondsLeft = -1, authorizationSecondsLeft = 20, expected = true),
            DeadlineModel(revocationSecondsLeft = 31, authorizationSecondsLeft = 631, expected = false),
            DeadlineModel(revocationSecondsLeft = 0, authorizationSecondsLeft = 600, expected = false),
            DeadlineModel(revocationSecondsLeft = -600, authorizationSecondsLeft = 0, expected = false),
        )
    }

    data class DeadlineModel(
        val revocationSecondsLeft: Long,
        val authorizationSecondsLeft: Long,
        val expected: Boolean,
    )

    private fun pendingWith(model: DeadlineModel): PolymarketPendingAuthorization {
        val pending = createPendingAuthorization()
        val nowSeconds = NOW_MILLIS / MILLIS_IN_SECOND
        return pending.copy(
            revocation = pending.revocation.copy(deadline = (nowSeconds + model.revocationSecondsLeft).toString()),
            authorization = pending.authorization.copy(
                deadline = (nowSeconds + model.authorizationSecondsLeft).toString(),
            ),
        )
    }

    private companion object {
        const val NOW_MILLIS = 1_800_000_000_000L
        const val MILLIS_IN_SECOND = 1_000L
    }
}