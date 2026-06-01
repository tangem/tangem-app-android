package com.tangem.lib.auth.session.internal

import arrow.core.None
import arrow.core.Some
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.common.services.secure.SecureStorage
import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultSessionTokensStoreTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(TokenApiResponse::class.java)

    private val sampleDto = TokenApiResponse(
        accessToken = "acc",
        accessTokenExpiresAt = "2023-11-14T22:13:20Z",
        refreshToken = "rt",
        refreshTokenExpiresAt = "2023-11-14T23:13:20Z",
        walletIds = listOf("w1", "w2"),
    )

    private val sampleDomain = SessionTokens(
        accessToken = "acc",
        accessTokenExpiresAt = Instant.parse("2023-11-14T22:13:20Z"),
        refreshToken = "rt",
        refreshTokenExpiresAt = Instant.parse("2023-11-14T23:13:20Z"),
        walletIds = listOf("w1", "w2"),
    )

    @Test
    fun `get returns None when nothing stored`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        every { storage.getAsString("session_tokens") } returns null

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)

        assertThat(store.get()).isEqualTo(None)
    }

    @Test
    fun `save round-trips through TokenApiResponse adapter`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        val captured = slot<String>()
        every { storage.store(eq("session_tokens"), capture(captured)) } returns Unit

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)
        store.save(sampleDomain)

        verify { storage.store("session_tokens", any()) }
        val decoded = adapter.fromJson(captured.captured)
        assertThat(decoded).isEqualTo(sampleDto)
    }

    @Test
    fun `get decodes TokenApiResponse and maps to domain`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        every { storage.getAsString("session_tokens") } returns adapter.toJson(sampleDto)

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)

        assertThat(store.get()).isEqualTo(Some(sampleDomain))
    }

    @Test
    fun `get returns None and clears corrupted entry`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        every { storage.getAsString("session_tokens") } returns "{not json"

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)

        assertThat(store.get()).isEqualTo(None)
        verify { storage.delete("session_tokens") }
    }

    @Test
    fun `clear removes the entry`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)
        store.clear()

        verify { storage.delete("session_tokens") }
    }
}