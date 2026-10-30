package com.tangem.data.staking.converters.error

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.StakingError.StakeKitApiError
import com.tangem.domain.staking.model.stakekit.StakingError.StakeKitApiError.ErrorDetails
import com.tangem.grow.datasource.stakekit.models.response.model.error.StakeKitErrorResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class StakeKitErrorConverterTest {

    private val converter = StakeKitErrorConverter(
        jsonAdapter = Moshi.Builder().build().adapter(StakeKitErrorResponse::class.java),
    )

    @Test
    fun `GIVEN api error with details WHEN convert THEN StakeKitApiError with parsed amount`() {
        // Arrange
        val json = """
            {"message":"Amount is below minimum","code":400,"path":"/v1/actions/enter",
            "details":{"amount":"0.5","yieldId":"y"}}
        """.trimIndent()

        // Act
        val actual = converter.convert(json)

        // Assert
        val expected = StakeKitApiError(
            message = "Amount is below minimum",
            code = 400,
            details = ErrorDetails(amount = BigDecimal("0.5")),
            methodName = "/v1/actions/enter",
        )
        assertThat(actual).isEqualTo(expected)
    }

    // Pins current behaviour: geo-block type and countryCode never reach the domain error
    @Test
    fun `GIVEN 403 geo payload WHEN convert THEN geo fields are dropped`() {
        // Arrange
        val json = """
            {"type":"GEO_LOCATION","message":"This action is not allowed in your region","code":403,
            "countryCode":"RU","regionCode":"MOW"}
        """.trimIndent()

        // Act
        val actual = converter.convert(json)

        // Assert
        val expected = StakeKitApiError(
            message = "This action is not allowed in your region",
            code = 403,
            details = null,
            methodName = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN empty json object WHEN convert THEN StakeKitApiError with null fields`() {
        // Act
        val actual = converter.convert("{}")

        // Assert
        assertThat(actual).isEqualTo(StakeKitApiError(message = null, code = null, details = null, methodName = null))
    }

    @Test
    fun `GIVEN json null literal WHEN convert THEN StakeKitUnknownError with raw string`() {
        // Act
        val actual = converter.convert("null")

        // Assert
        assertThat(actual).isEqualTo(StakingError.StakeKitUnknownError("null"))
    }

    @Test
    fun `GIVEN html body WHEN convert THEN StakeKitUnknownError with raw string`() {
        // Act
        val actual = converter.convert("<html>502 Bad Gateway</html>")

        // Assert
        assertThat(actual).isEqualTo(StakingError.StakeKitUnknownError("<html>502 Bad Gateway</html>"))
    }
}