package com.tangem.lib.auth.http

import arrow.core.None
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
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.datetime.Instant
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import retrofit2.Invocation
import java.lang.reflect.Method

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DpopAuthorizationInterceptorTest {

    private val store: SessionTokensStore = mockk()
    private val proofFactory: DpopProofFactory = mockk()
    private val refresher: SessionTokenRefresher = mockk()

    private val interceptor = DpopAuthorizationInterceptor(store, proofFactory, Lazy { refresher })

    @BeforeEach
    fun setup() {
        clearMocks(store, proofFactory, refresher)
    }

    private val storedTokens = SessionTokens(
        accessToken = "old-access",
        accessTokenExpiresAt = Instant.fromEpochSeconds(1_700_000_000),
        refreshToken = "rt",
        refreshTokenExpiresAt = Instant.fromEpochSeconds(1_700_003_600),
        walletIds = emptyList(),
    )

    @Test
    fun `@RequiresDpopProof method gets Authorization and DPoP headers`() {
        coEvery { store.get() } returns Some(storedTokens)
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("proof-jwt")

        val proceeded = slot<Request>()
        val chain = chain(request(dpop = true), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isEqualTo("DPoP old-access")
        assertThat(proceeded.captured.header("DPoP")).isEqualTo("proof-jwt")
    }

    @Test
    fun `@RequiresSessionAuth (umbrella) method gets headers — token resolved via ensureSession`() {
        coEvery { refresher.ensureSession() } returns storedTokens.right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("proof-jwt")

        val proceeded = slot<Request>()
        val chain = chain(request(sessionAuth = true), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isEqualTo("DPoP old-access")
        assertThat(proceeded.captured.header("DPoP")).isEqualTo("proof-jwt")
        // Session-protected endpoints must not read the store directly (that's what sent a stale token
        // while a refresh was in flight); they go through ensureSession, which joins any in-flight refresh.
        coVerify(exactly = 0) { store.get() }
    }

    @Test
    fun `@RequiresSessionAuth method sends the freshly refreshed token, not the stale stored one`() {
        // ensureSession refreshed a stale/in-flight token and returns the new one.
        val refreshed = storedTokens.copy(accessToken = "new-access")
        coEvery { refresher.ensureSession() } returns refreshed.right()
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("proof-jwt")

        val proceeded = slot<Request>()
        val chain = chain(request(sessionAuth = true), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isEqualTo("DPoP new-access")
        assertThat(proceeded.captured.header("DPoP")).isEqualTo("proof-jwt")
    }

    @Test
    fun `@RequiresSessionRefresh-only method does NOT get DPoP headers`() {
        val proceeded = slot<Request>()
        val chain = chain(request(sessionRefresh = true), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isNull()
        assertThat(proceeded.captured.header("DPoP")).isNull()
        coVerify(exactly = 0) { proofFactory.create(any(), any(), any()) }
    }

    @Test
    fun `annotated request without access token passes through unmodified`() {
        coEvery { store.get() } returns None

        val proceeded = slot<Request>()
        val chain = chain(request(dpop = true), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isNull()
        assertThat(proceeded.captured.header("DPoP")).isNull()
        coVerify(exactly = 0) { proofFactory.create(any(), any(), any()) }
    }

    @Test
    fun `unannotated request passes through unchanged — proof factory never invoked`() {
        val proceeded = slot<Request>()
        val chain = chain(request(), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isNull()
        assertThat(proceeded.captured.header("DPoP")).isNull()
        coVerify(exactly = 0) { proofFactory.create(any(), any(), any()) }
    }

    @Test
    fun `request without Invocation tag (not via Retrofit) is treated as unannotated`() {
        val original = Request.Builder().url("https://example.com/api/v1/foo").build()
        val proceeded = slot<Request>()
        val chain = chain(original, proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("DPoP")).isNull()
        coVerify(exactly = 0) { proofFactory.create(any(), any(), any()) }
    }

    @Test
    fun `proof generation failure on annotated request passes through without headers`() {
        coEvery { store.get() } returns Some(storedTokens)
        coEvery { proofFactory.create(any(), any(), any()) } returns None

        val proceeded = slot<Request>()
        val chain = chain(request(dpop = true), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isNull()
        assertThat(proceeded.captured.header("DPoP")).isNull()
    }

    @Test
    fun `GIVEN session-auth request with static Bearer already set WHEN session valid THEN Bearer replaced by DPoP`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns storedTokens.right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("proof-jwt")

        val proceeded = slot<Request>()
        val chain = chain(request(sessionAuth = true, bearerHeader = "Bearer gasless-key"), proceeded)

        // Act
        interceptor.intercept(chain)

        // Assert
        assertThat(proceeded.captured.headers("Authorization")).containsExactly("DPoP old-access")
        assertThat(proceeded.captured.header("DPoP")).isEqualTo("proof-jwt")
    }

    @Test
    fun `GIVEN session-auth request with static Bearer WHEN session cannot be resolved THEN Bearer kept and no DPoP sent`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns SessionRefreshError.DeviceKeyUnavailable.left()

        val proceeded = slot<Request>()
        val chain = chain(request(sessionAuth = true, bearerHeader = "Bearer gasless-key"), proceeded)

        // Act
        interceptor.intercept(chain)

        // Assert
        assertThat(proceeded.captured.headers("Authorization")).containsExactly("Bearer gasless-key")
        assertThat(proceeded.captured.header("DPoP")).isNull()
        coVerify(exactly = 0) { proofFactory.create(any(), any(), any()) }
    }

    @Test
    fun `GIVEN session-auth request with static Bearer WHEN proof generation fails THEN Bearer kept and no DPoP sent`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns storedTokens.right()
        coEvery { proofFactory.create(any(), any(), any()) } returns None

        val proceeded = slot<Request>()
        val chain = chain(request(sessionAuth = true, bearerHeader = "Bearer gasless-key"), proceeded)

        // Act
        interceptor.intercept(chain)

        // Assert
        assertThat(proceeded.captured.headers("Authorization")).containsExactly("Bearer gasless-key")
        assertThat(proceeded.captured.header("DPoP")).isNull()
    }

    @Test
    fun `GIVEN 403 on session-auth request WHEN refresh succeeds THEN retried once with refreshed DPoP`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns storedTokens.right()
        coEvery { refresher.refresh() } returns storedTokens.copy(accessToken = "new-access").right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("new-proof")

        val req = request(sessionAuth = true)
        val forbidden = response(req, code = 403)
        val ok = response(req, code = 200)
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } answers {
            if (firstArg<Request>().header("Authorization") == "DPoP old-access") forbidden else ok
        }

        // Act
        val result = interceptor.intercept(chain)

        // Assert
        assertThat(result.code).isEqualTo(200)
        verify(exactly = 2) { chain.proceed(any()) }
        coVerify(exactly = 1) { refresher.refresh() }
    }

    @Test
    fun `GIVEN 403 on session-auth request WHEN refresh fails THEN original 403 is returned without a second request`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns storedTokens.right()
        coEvery { refresher.refresh() } returns SessionRefreshError.DeviceKeyUnavailable.left()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")

        val req = request(sessionAuth = true)
        val forbidden = response(req, code = 403)
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } returns forbidden

        // Act
        val result = interceptor.intercept(chain)

        // Assert
        assertThat(result.code).isEqualTo(403)
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun `GIVEN 403 on session-auth request WHEN proof after refresh fails THEN original 403 is returned without a second request`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns storedTokens.right()
        coEvery { refresher.refresh() } returns storedTokens.copy(accessToken = "new-access").right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")
        coEvery { proofFactory.create(any(), any(), "new-access") } returns None

        val req = request(sessionAuth = true)
        val forbidden = response(req, code = 403)
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } returns forbidden

        // Act
        val result = interceptor.intercept(chain)

        // Assert
        assertThat(result.code).isEqualTo(403)
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun `GIVEN 403 on @RequiresDpopProof-only request THEN no retry is attempted`() {
        // Arrange
        coEvery { store.get() } returns Some(storedTokens)
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")

        val req = request(dpop = true)
        val forbidden = response(req, code = 403)
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } returns forbidden

        // Act
        val result = interceptor.intercept(chain)

        // Assert
        assertThat(result.code).isEqualTo(403)
        verify(exactly = 1) { chain.proceed(any()) }
        coVerify(exactly = 0) { refresher.refresh() }
    }

    @Test
    fun `retried request after a 403 carries the session-auth retry marker`() {
        // Arrange
        coEvery { refresher.ensureSession() } returns storedTokens.right()
        coEvery { refresher.refresh() } returns storedTokens.copy(accessToken = "new-access").right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("new-proof")

        val req = request(sessionAuth = true)
        val sentRequests = mutableListOf<Request>()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } answers {
            val sent = firstArg<Request>()
            sentRequests += sent
            if (sent.header("Authorization") == "DPoP old-access") response(sent, code = 403) else response(sent, code = 200)
        }

        // Act
        interceptor.intercept(chain)

        // Assert
        assertThat(sentRequests).hasSize(2)
        assertThat(sentRequests[1].isSessionAuthRetry()).isTrue()
    }

    @Test
    fun `GIVEN 403 whose request is already marked as a session-auth retry THEN no further retry is attempted`() {
        // Arrange — mirrors a 401-then-403 sequence: SessionAuthenticator already retried once for
        // the 401 (within the same chain.proceed() call) and marked the retried request. This
        // interceptor must not add a second, independent retry on top of that one.
        coEvery { refresher.ensureSession() } returns storedTokens.right()
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("old-proof")

        val req = request(sessionAuth = true)
        val alreadyRetried = req.markSessionAuthRetried()
        val forbidden = response(alreadyRetried, code = 403)
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } returns forbidden

        // Act
        val result = interceptor.intercept(chain)

        // Assert
        assertThat(result.code).isEqualTo(403)
        verify(exactly = 1) { chain.proceed(any()) }
        coVerify(exactly = 0) { refresher.refresh() }
    }

    private fun response(request: Request, code: Int): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("test")
        .body("".toResponseBody(null))
        .build()

    private fun request(
        dpop: Boolean = false,
        sessionRefresh: Boolean = false,
        sessionAuth: Boolean = false,
        bearerHeader: String? = null,
    ): Request {
        val builder = Request.Builder().url("https://example.com/api/v1/foo")
        bearerHeader?.let { builder.header("Authorization", it) }
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

    private fun chain(request: Request, captureSlot: CapturingSlot<Request>): Interceptor.Chain {
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("ok")
            .build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(capture(captureSlot)) } returns response
        return chain
    }
}