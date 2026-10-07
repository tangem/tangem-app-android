package com.tangem.core.socket.transport

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class OkHttpSocketTransportTest {

    @Test
    fun `GIVEN client without ping interval WHEN transport created THEN it is rejected`() {
        // Arrange
        val client = OkHttpClient()

        // Act
        val error = runCatching { OkHttpSocketTransport(client) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN client with ping interval WHEN transport created THEN it is accepted`() {
        // Arrange
        val client = OkHttpClient.Builder().pingInterval(15, TimeUnit.SECONDS).build()

        // Act
        val error = runCatching { OkHttpSocketTransport(client) }.exceptionOrNull()

        // Assert
        assertThat(error).isNull()
    }
}