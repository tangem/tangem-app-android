package com.tangem.lib.auth.http

import arrow.core.None
import arrow.core.Some
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.auth.RequiresSessionAuth
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.lib.auth.session.SessionTokensStore
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.datetime.Instant
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import retrofit2.Invocation
import java.lang.reflect.Method

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DpopAuthorizationInterceptorTest {

    private val store: SessionTokensStore = mockk()
    private val proofFactory: DpopProofFactory = mockk()

    private val interceptor = DpopAuthorizationInterceptor(store, proofFactory)

    @BeforeEach
    fun setup() {
        clearMocks(store, proofFactory)
    }

    private val storedTokens = SessionTokens(
        accessToken = "old-access",
        accessTokenExpiresAt = Instant.fromEpochSeconds(1_700_000_000),
        refreshToken = "rt",
        refreshTokenExpiresAt = Instant.fromEpochSeconds(1_700_003_600),
        walletIds = emptyList(),
    )

    @Test
    fun `annotated request gets Authorization and DPoP headers`() {
        coEvery { store.get() } returns Some(storedTokens)
        coEvery { proofFactory.create(any(), any(), "old-access") } returns Some("proof-jwt")

        val proceeded = slot<Request>()
        val chain = chain(request(annotated = true), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isEqualTo("DPoP old-access")
        assertThat(proceeded.captured.header("DPoP")).isEqualTo("proof-jwt")
    }

    @Test
    fun `annotated request without access token passes through unmodified`() {
        coEvery { store.get() } returns None

        val proceeded = slot<Request>()
        val chain = chain(request(annotated = true), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isNull()
        assertThat(proceeded.captured.header("DPoP")).isNull()
        coVerify(exactly = 0) { proofFactory.create(any(), any(), any()) }
    }

    @Test
    fun `unannotated request passes through unchanged — proof factory never invoked`() {
        val original = request(annotated = false)
        val proceeded = slot<Request>()
        val chain = chain(original, proceeded)

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
        val chain = chain(request(annotated = true), proceeded)

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("Authorization")).isNull()
        assertThat(proceeded.captured.header("DPoP")).isNull()
    }

    private fun request(annotated: Boolean): Request {
        val builder = Request.Builder().url("https://example.com/api/v1/foo")
        builder.tag(Invocation::class.java, invocationWithAnnotation(annotated))
        return builder.build()
    }

    private fun invocationWithAnnotation(annotated: Boolean): Invocation {
        val method = mockk<Method>()
        every { method.isAnnotationPresent(RequiresSessionAuth::class.java) } returns annotated
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