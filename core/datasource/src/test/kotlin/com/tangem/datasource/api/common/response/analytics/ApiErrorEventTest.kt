package com.tangem.datasource.api.common.response.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ApiErrorEventTest {

    @Test
    fun `GIVEN error body with hex identifiers WHEN fromErrorBody THEN they are masked`() {
        // Arrange
        val body = """{"error":"wallet 0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb not found","code":404}"""

        // Act
        val event = ApiErrorEvent.fromErrorBody(endpoint = "api.tangem.org/user-tokens", code = 404, errorBody = body)

        // Assert
        assertThat(event.params["Endpoint"]).isEqualTo("api.tangem.org/user-tokens")
        assertThat(event.params["Code"]).isEqualTo("404")
        assertThat(event.params["Message"]).doesNotContain("D1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb")
        assertThat(event.params["Message"]).contains("not found")
    }

    @Test
    fun `GIVEN oversized error body WHEN fromErrorBody THEN message is capped`() {
        // Arrange
        val body = "x".repeat(5_000)

        // Act
        val event = ApiErrorEvent.fromErrorBody(endpoint = "host/path", code = 500, errorBody = body)

        // Assert
        assertThat(event.params["Message"]!!.length).isEqualTo(201)
        assertThat(event.params["Message"]).endsWith("…")
    }

    @Test
    fun `GIVEN short plain body WHEN fromErrorBody THEN message is unchanged`() {
        // Act
        val event = ApiErrorEvent.fromErrorBody(endpoint = "host/path", code = 400, errorBody = "Bad Request")

        // Assert
        assertThat(event.params["Message"]).isEqualTo("Bad Request")
    }
}
