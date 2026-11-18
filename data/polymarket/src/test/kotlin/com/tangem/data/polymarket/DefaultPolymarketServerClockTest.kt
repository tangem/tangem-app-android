package com.tangem.data.polymarket

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketServerClockTest {

    @Test
    fun `GIVEN no backend answer yet WHEN now is read THEN it is the device time`() {
        // Arrange
        val clock = DefaultPolymarketServerClock(deviceClock = { DEVICE_NOW })

        // Act
        val actual = clock.nowMillis()

        // Assert
        assertThat(actual).isEqualTo(DEVICE_NOW)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN backend answer WHEN recorded THEN now follows its date or stays on the device time`(model: Model) {
        // Arrange
        val clock = DefaultPolymarketServerClock(deviceClock = { DEVICE_NOW })

        // Act
        clock.record(model.response)

        // Assert
        assertThat(clock.nowMillis()).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        Model(case = "answer with the date", response = success(date = SERVER_DATE), expected = SERVER_NOW),
        Model(
            case = "failed answer with the date",
            response = ApiResponse.Error(
                cause = ApiResponseError.NetworkException(),
                headers = mapOf("date" to listOf(SERVER_DATE)),
            ),
            expected = SERVER_NOW,
        ),
        Model(case = "no date", response = success(date = null), expected = DEVICE_NOW),
        Model(case = "unreadable date", response = success(date = "not a date"), expected = DEVICE_NOW),
        Model(
            case = "no value under the header",
            response = ApiResponse.Success(data = Unit, headers = mapOf("date" to emptyList())),
            expected = DEVICE_NOW,
        ),
    )

    data class Model(val case: String, val response: ApiResponse<*>, val expected: Long)

    @Test
    fun `GIVEN recorded answer WHEN the device clock moves on THEN now moves with it`() {
        // Arrange
        var deviceNow = DEVICE_NOW
        val clock = DefaultPolymarketServerClock(deviceClock = { deviceNow })
        clock.record(success(date = SERVER_DATE))

        // Act
        deviceNow += ELAPSED

        // Assert
        assertThat(clock.nowMillis()).isEqualTo(SERVER_NOW + ELAPSED)
    }

    @Test
    fun `GIVEN two answers WHEN recorded THEN the later one sets the time`() {
        // Arrange
        val clock = DefaultPolymarketServerClock(deviceClock = { DEVICE_NOW })
        clock.record(success(date = "Thu, 17 Sep 2026 12:00:00 GMT"))

        // Act
        clock.record(success(date = SERVER_DATE))

        // Assert
        assertThat(clock.nowMillis()).isEqualTo(SERVER_NOW)
    }

    @ParameterizedTest
    @ValueSource(longs = [-1_999L, -400L, 0L, 1_999L])
    fun `GIVEN answer within the noise of the current time WHEN recorded THEN the time is kept`(shiftMillis: Long) {
        // Arrange
        val clock = DefaultPolymarketServerClock(deviceClock = { SERVER_NOW + shiftMillis })

        // Act
        clock.record(success(date = SERVER_DATE))

        // Assert
        assertThat(clock.nowMillis()).isEqualTo(SERVER_NOW + shiftMillis)
    }

    @ParameterizedTest
    @ValueSource(longs = [-2_000L, 2_000L, 90_000L])
    fun `GIVEN answer beyond the noise of the current time WHEN recorded THEN now follows it`(shiftMillis: Long) {
        // Arrange
        val clock = DefaultPolymarketServerClock(deviceClock = { SERVER_NOW + shiftMillis })

        // Act
        clock.record(success(date = SERVER_DATE))

        // Assert
        assertThat(clock.nowMillis()).isEqualTo(SERVER_NOW)
    }

    @Test
    fun `GIVEN recorded answer WHEN an unreadable one follows THEN the time is kept`() {
        // Arrange
        val clock = DefaultPolymarketServerClock(deviceClock = { DEVICE_NOW })
        clock.record(success(date = SERVER_DATE))

        // Act
        clock.record(success(date = null))

        // Assert
        assertThat(clock.nowMillis()).isEqualTo(SERVER_NOW)
    }

    private fun success(date: String?): ApiResponse<Unit> = ApiResponse.Success(
        data = Unit,
        headers = date?.let { mapOf("date" to listOf(it)) }.orEmpty(),
    )

    private companion object {
        const val DEVICE_NOW = 1_787_000_000_000L
        const val SERVER_DATE = "Thu, 17 Sep 2026 12:34:51 GMT"
        const val SERVER_NOW = 1_789_648_491_000L
        const val ELAPSED = 7_500L
    }
}