package com.tangem.datasource.utils

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Test

internal class ForeignHostHeadersStripInterceptorTest {

    private val interceptor = ForeignHostHeadersStripInterceptor(
        apiHost = "api.tangem.org",
        headerNames = setOf("api-key", "card_id"),
    )

    @Test
    fun `GIVEN request to the api host WHEN intercept THEN headers are kept`() {
        val sent = proceedWith(url = "https://api.tangem.org/v1/quotes")

        assertThat(sent.header("api-key")).isEqualTo("secret")
        assertThat(sent.header("card_id")).isEqualTo("CB01")
        assertThat(sent.header("Accept")).isEqualTo("application/json")
    }

    @Test
    fun `GIVEN request redirected to another host WHEN intercept THEN configured headers are removed`() {
        val sent = proceedWith(url = "https://cdn.example.com/v1/quotes")

        assertThat(sent.header("api-key")).isNull()
        assertThat(sent.header("card_id")).isNull()
        assertThat(sent.header("Accept")).isEqualTo("application/json")
    }

    @Test
    fun `GIVEN host differing only in case WHEN intercept THEN treated as the api host`() {
        val sent = proceedWith(url = "https://API.Tangem.org/v1/quotes")

        assertThat(sent.header("api-key")).isEqualTo("secret")
    }

    private fun proceedWith(url: String): Request {
        val request = Request.Builder()
            .url(url)
            .header("api-key", "secret")
            .header("card_id", "CB01")
            .header("Accept", "application/json")
            .build()
        val proceeded = slot<Request>()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns request
            every { proceed(capture(proceeded)) } returns mockk<Response>()
        }

        interceptor.intercept(chain)

        return proceeded.captured
    }
}
