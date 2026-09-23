package com.tangem.lib.auth.session.internal

import arrow.core.None
import arrow.core.Some
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.common.services.secure.SecureStorage
import com.tangem.lib.auth.api.models.response.TokenApiResponse
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
import java.util.Base64
import kotlin.time.Duration.Companion.minutes

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
    fun `save discards a set issued before the stored one`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        storeHolds(storage, jwtIssuedAt(seconds = 2_000))

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)
        // a shorter lifetime, so the expiry alone would not reveal which one is newer
        store.save(
            sampleDomain.copy(
                accessToken = jwtIssuedAt(seconds = 1_000),
                accessTokenExpiresAt = sampleDomain.accessTokenExpiresAt.plus(1.minutes),
            ),
        )

        verify(exactly = 0) { storage.store(any<String>(), any<String>()) }
    }

    @Test
    fun `save keeps a set issued after the stored one`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        storeHolds(storage, jwtIssuedAt(seconds = 1_000))
        val captured = slot<String>()
        every { storage.store(eq("session_tokens"), capture(captured)) } returns Unit
        val reissued = jwtIssuedAt(seconds = 2_000)

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)
        store.save(
            sampleDomain.copy(
                accessToken = reissued,
                accessTokenExpiresAt = sampleDomain.accessTokenExpiresAt.minus(1.minutes),
            ),
        )

        assertThat(adapter.fromJson(captured.captured)?.accessToken).isEqualTo(reissued)
    }

    @Test
    fun `save keeps the last arrival when both sets name the same second`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        storeHolds(storage, jwtIssuedAt(seconds = 1_000))
        val captured = slot<String>()
        every { storage.store(eq("session_tokens"), capture(captured)) } returns Unit
        val sameInstant = jwtIssuedAt(seconds = 1_000, subject = "other")

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)
        store.save(sampleDomain.copy(accessToken = sameInstant))

        // iat cannot separate them and the client has nothing better to go on; see save's contract
        assertThat(adapter.fromJson(captured.captured)?.accessToken).isEqualTo(sameInstant)
    }

    @Test
    fun `get does not clear an entry a save has replaced meanwhile`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        val valid = adapter.toJson(sampleDto)
        // the corrupt blob is read first, then replaced before the cleanup gets to run
        every { storage.getAsString("session_tokens") } returnsMany listOf("{not json", valid)

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)
        val actual = store.get()

        assertThat(actual).isEqualTo(None)
        verify(exactly = 0) { storage.delete(any()) }
    }

    @Test
    fun `save returns the surviving set when the incoming one is discarded`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        val stored = jwtIssuedAt(seconds = 2_000)
        storeHolds(storage, stored)

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)
        val inEffect = store.save(sampleDomain.copy(accessToken = jwtIssuedAt(seconds = 1_000)))

        // callers retry with what they get back, so it must be the set the store actually holds
        assertThat(inEffect.accessToken).isEqualTo(stored)
    }

    @Test
    fun `save keeps a set whose issuance cannot be read`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)
        storeHolds(storage, jwtIssuedAt(seconds = 2_000))
        val captured = slot<String>()
        every { storage.store(eq("session_tokens"), capture(captured)) } returns Unit

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)
        store.save(sampleDomain.copy(accessToken = "not-a-jwt"))

        assertThat(adapter.fromJson(captured.captured)?.accessToken).isEqualTo("not-a-jwt")
    }

    private fun storeHolds(storage: SecureStorage, accessToken: String) {
        every { storage.getAsString("session_tokens") } returns adapter.toJson(
            sampleDto.copy(accessToken = accessToken),
        )
    }

    private fun jwtIssuedAt(seconds: Long, subject: String = "device"): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = encoder.encodeToString("""{"iat":$seconds,"sub":"$subject"}""".toByteArray())

        return "$header.$payload.signature"
    }

    @Test
    fun `clear removes the entry`() = runTest {
        val storage = mockk<SecureStorage>(relaxed = true)

        val store = DefaultSessionTokensStore(storage, moshi, dispatchers)
        store.clear()

        verify { storage.delete("session_tokens") }
    }
}