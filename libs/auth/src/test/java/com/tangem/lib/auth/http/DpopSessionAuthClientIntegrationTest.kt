package com.tangem.lib.auth.http

import arrow.core.Some
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.lib.auth.api.RequiresDpopProof
import com.tangem.lib.auth.api.RequiresSessionAuth
import com.tangem.lib.auth.api.RequiresSessionRefresh
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.lib.auth.session.SessionRefreshError
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.lib.auth.session.SessionTokensStore
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Invocation
import java.lang.reflect.Method

/**
 * Wires [DpopAuthorizationInterceptor] and [SessionAuthenticator] into a real [OkHttpClient] —
 * the same two hooks `DefaultRetrofitFactory.applySessionAuth` installs — against a real local
 * server. A test built around a mocked `Interceptor.Chain` cannot catch a wiring bug where OkHttp
 * itself never reaches a hook for a given response code (e.g. `Authenticator.authenticate()` is
 * only invoked by OkHttp for 401, never for 403); only a real client proves that end to end.
 */
class DpopSessionAuthClientIntegrationTest {

    private val store: SessionTokensStore = mockk()
    private val proofFactory: DpopProofFactory = mockk()
    private val refresher: SessionTokenRefresher = mockk()

    private lateinit var server: MockWebServer

    private val client: OkHttpClient by lazy {
        val authorizationInterceptor = DpopAuthorizationInterceptor(store, proofFactory, Lazy { refresher })
        val authenticator = SessionAuthenticator(refresher, proofFactory)
        OkHttpClient.Builder()
            .addInterceptor(authorizationInterceptor)
            .authenticator { route, response -> authenticator.authenticate(route, response) }
            .build()
    }

    private val oldTokens = SessionTokens(
        accessToken = "old-access",
        accessTokenExpiresAt = Instant.fromEpochSeconds(1_700_000_000),
        refreshToken = "rt",
        refreshTokenExpiresAt = Instant.fromEpochSeconds(1_700_003_600),
        walletIds = emptyList(),
    )
    private val newTokens = oldTokens.copy(accessToken = "new-access", refreshToken = "rt-2")

    @BeforeEach
    fun startServer() {
        server = MockWebServer().apply { start() }
    }

    @AfterEach
    fun stopServer() {
        server.shutdown()
    }

    @Test
    fun `GIVEN 403 then success WHEN session-auth request sent THEN interceptor retries once with refreshed DPoP`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns oldTokens.right()
        coEvery { refresher.refresh() } returns newTokens.right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("new-proof")
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        // Act
        val response = client.newCall(request(sessionAuth = true)).execute()

        // Assert
        assertThat(response.code).isEqualTo(200)
        assertThat(server.requestCount).isEqualTo(2)
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("DPoP old-access")
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("DPoP new-access")
        response.close()
    }

    @Test
    fun `GIVEN 403 twice WHEN session-auth request sent THEN only one retry happens and 403 surfaces`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns oldTokens.right()
        coEvery { refresher.refresh() } returns newTokens.right()
        coEvery { proofFactory.create(any(), any(), any()) } returns Some("proof")
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setResponseCode(403))

        // Act
        val response = client.newCall(request(sessionAuth = true)).execute()

        // Assert
        assertThat(response.code).isEqualTo(403)
        assertThat(server.requestCount).isEqualTo(2)
        response.close()
    }

    @Test
    fun `GIVEN 403 WHEN refresh fails THEN no second request is sent and 403 surfaces`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns oldTokens.right()
        coEvery { refresher.refresh() } returns SessionRefreshError.DeviceKeyUnavailable.left()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")
        server.enqueue(MockResponse().setResponseCode(403))

        // Act
        val response = client.newCall(request(sessionAuth = true)).execute()

        // Assert
        assertThat(response.code).isEqualTo(403)
        assertThat(server.requestCount).isEqualTo(1)
        response.close()
    }

    @Test
    fun `GIVEN 401 then success WHEN session-auth request sent THEN Authenticator still retries once`() {
        // Arrange — regression: the interceptor's own 403 handling must not break the existing 401 path.
        coEvery { refresher.ensureSession() } returns oldTokens.right()
        coEvery { refresher.refresh() } returns newTokens.right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("new-proof")
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        // Act
        val response = client.newCall(request(sessionAuth = true)).execute()

        // Assert
        assertThat(response.code).isEqualTo(200)
        assertThat(server.requestCount).isEqualTo(2)
        response.close()
    }

    @Test
    fun `GIVEN 401 then 403 WHEN session-auth request sent THEN only one retry happens across both mechanisms`() {
        // Arrange — the Authenticator retries the 401 and marks its retry; this interceptor must see
        // that marker and not add its own, independent retry on top when that retry comes back 403.
        // A third enqueued response proves (rather than hangs on) a would-be extra request.
        coEvery { refresher.ensureSession() } returns oldTokens.right()
        coEvery { refresher.refresh() } returns newTokens.right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("new-proof")
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setResponseCode(200).setBody("unused if the fix holds"))

        // Act
        val response = client.newCall(request(sessionAuth = true)).execute()

        // Assert
        assertThat(response.code).isEqualTo(403)
        assertThat(server.requestCount).isEqualTo(2)
        response.close()
    }

    @Test
    fun `GIVEN 403 then 401 WHEN session-auth request sent THEN only one retry happens across both mechanisms`() {
        // Arrange — this interceptor retries the 403 and marks its retry; the Authenticator must see
        // that marker and not add its own, independent retry on top when that retry comes back 401.
        coEvery { refresher.ensureSession() } returns oldTokens.right()
        coEvery { refresher.refresh() } returns newTokens.right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("new-proof")
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("unused if the fix holds"))

        // Act
        val response = client.newCall(request(sessionAuth = true)).execute()

        // Assert
        assertThat(response.code).isEqualTo(401)
        assertThat(server.requestCount).isEqualTo(2)
        response.close()
    }

    @Test
    fun `GIVEN 403 WHEN request has no session-refresh annotation THEN no retry happens`() {
        // Arrange
        server.enqueue(MockResponse().setResponseCode(403))

        // Act
        val response = client.newCall(request()).execute()

        // Assert
        assertThat(response.code).isEqualTo(403)
        assertThat(server.requestCount).isEqualTo(1)
        response.close()
    }

    private fun request(
        dpop: Boolean = false,
        sessionRefresh: Boolean = false,
        sessionAuth: Boolean = false,
    ): Request {
        val builder = Request.Builder().url(server.url("/foo"))
        builder.tag(Invocation::class.java, invocationWith(dpop, sessionRefresh, sessionAuth))
        return builder.build()
    }

    private fun invocationWith(dpop: Boolean, sessionRefresh: Boolean, sessionAuth: Boolean): Invocation {
        val method = mockk<Method>()
        every { method.isAnnotationPresent(RequiresDpopProof::class.java) } returns dpop
        every { method.isAnnotationPresent(RequiresSessionRefresh::class.java) } returns sessionRefresh
        every { method.isAnnotationPresent(RequiresSessionAuth::class.java) } returns sessionAuth
        val invocation = mockk<Invocation>()
        every { invocation.method() } returns method
        return invocation
    }
}