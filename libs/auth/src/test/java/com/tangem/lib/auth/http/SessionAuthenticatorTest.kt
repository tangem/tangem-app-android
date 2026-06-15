package com.tangem.lib.auth.http

import arrow.core.None
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.auth.RequiresDpopProof
import com.tangem.datasource.api.auth.RequiresSessionAuth
import com.tangem.datasource.api.auth.RequiresSessionRefresh
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.SessionRefreshError
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.lib.auth.session.SessionTokens
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import retrofit2.Invocation
import java.lang.reflect.Method

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionAuthenticatorTest {

    private val refresher: SessionTokenRefresher = mockk()
    private val proofFactory: DpopProofFactory = mockk()

    private val authenticator = SessionAuthenticator(refresher, proofFactory)

    private val refreshedTokens = SessionTokens(
        accessToken = "new-access",
        accessTokenExpiresAt = Instant.fromEpochSeconds(1_700_000_000),
        refreshToken = "rt-2",
        refreshTokenExpiresAt = Instant.fromEpochSeconds(1_700_003_600),
        walletIds = emptyList(),
    )

    @Test
    fun `401 on @RequiresSessionRefresh triggers refresh and retries with new headers`() {
        coEvery { refresher.refresh() } returns refreshedTokens.right()
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("fresh-proof")

        val retried = authenticator.authenticate(
            route = null,
            response = response(code = 401, sessionRefresh = true),
        )

        assertThat(retried).isNotNull()
        assertThat(retried!!.header("Authorization")).isEqualTo("DPoP new-access")
        assertThat(retried.header("DPoP")).isEqualTo("fresh-proof")
    }

    @Test
    fun `401 on @RequiresSessionAuth (umbrella) triggers refresh — covers refresh path transitively`() {
        coEvery { refresher.refresh() } returns refreshedTokens.right()
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("fresh-proof")

        val retried = authenticator.authenticate(
            route = null,
            response = response(code = 401, sessionAuth = true),
        )

        assertThat(retried).isNotNull()
    }

    @Test
    fun `401 on @RequiresDpopProof-only method does NOT trigger refresh — prevents recursion`() {
        val retried = authenticator.authenticate(
            route = null,
            response = response(code = 401, dpop = true),
        )

        assertThat(retried).isNull()
    }

    @Test
    fun `403 also triggers refresh`() {
        coEvery { refresher.refresh() } returns refreshedTokens.right()
        coEvery { proofFactory.create(any(), any(), "new-access") } returns Some("fresh-proof")

        val retried = authenticator.authenticate(
            route = null,
            response = response(code = 403, sessionRefresh = true),
        )

        assertThat(retried).isNotNull()
    }

    @Test
    fun `other 4xx codes are passed through`() {
        val retried = authenticator.authenticate(
            route = null,
            response = response(code = 404, sessionRefresh = true),
        )

        assertThat(retried).isNull()
    }

    @Test
    fun `prior response present means we already retried — give up`() {
        val first = response(code = 401, sessionRefresh = true)
        val second = response(code = 401, sessionRefresh = true, priorResponse = first)

        val retried = authenticator.authenticate(route = null, response = second)

        assertThat(retried).isNull()
    }

    @Test
    fun `unannotated request 401 is passed through without refresh`() {
        val retried = authenticator.authenticate(route = null, response = response(code = 401))

        assertThat(retried).isNull()
    }

    @Test
    fun `refresh failure gives up`() {
        coEvery { refresher.refresh() } returns SessionRefreshError.Api(AuthError.NetworkError).left()

        val retried = authenticator.authenticate(
            route = null,
            response = response(code = 401, sessionRefresh = true),
        )

        assertThat(retried).isNull()
    }

    @Test
    fun `proof generation None result gives up`() {
        coEvery { refresher.refresh() } returns refreshedTokens.right()
        coEvery { proofFactory.create(any(), any(), any()) } returns None

        val retried = authenticator.authenticate(
            route = null,
            response = response(code = 401, sessionRefresh = true),
        )

        assertThat(retried).isNull()
    }

    private fun response(
        code: Int,
        dpop: Boolean = false,
        sessionRefresh: Boolean = false,
        sessionAuth: Boolean = false,
        priorResponse: Response? = null,
    ): Response {
        val builder = Request.Builder().url("https://example.com/api/v1/foo")
        builder.tag(Invocation::class.java, invocationWith(dpop, sessionRefresh, sessionAuth))
        val request = builder.build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .apply { if (priorResponse != null) priorResponse(priorResponse) }
            .build()
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