package com.tangem.sdk.api.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.models.AnalyticsParam
import org.junit.jupiter.api.Test

internal class CardSdkErrorConverterTest {

    private val converter = CardSdkErrorConverter()

    @Test
    fun `GIVEN user cancelled WHEN convert THEN returns empty params`() {
        // Act
        val actual = converter.convert(TangemSdkError.UserCancelled())

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN card sdk error WHEN convert THEN returns card sdk error params`() {
        // Arrange
        val error = TangemSdkError.TagLost()

        // Act
        val actual = converter.convert(error)

        // Assert
        val expected = mapOf(
            AnalyticsParam.ERROR_KEY to AnalyticsParam.Error.CardSdk.value,
            AnalyticsParam.ERROR_CODE to error.code.toString(),
            AnalyticsParam.ERROR_DESCRIPTION to error.toString(),
        )
        assertThat(actual).isEqualTo(expected)
    }
}