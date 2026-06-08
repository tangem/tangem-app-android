package com.tangem.lib.auth.session.internal

import arrow.core.None
import arrow.core.Some
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.auth.AuthApi
import com.tangem.datasource.api.auth.models.request.AuthApiRequest
import com.tangem.datasource.api.auth.models.request.RefreshApiRequest
import com.tangem.datasource.api.auth.models.response.NonceApiResponse
import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.SessionRefreshError
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultSessionTokenRefresherTest {

    private val authApi: AuthApi = mockk()
    private val store: SessionTokensStore = mockk(relaxUnitFun = true)
    private val deviceKeyManager: DeviceKeyManager = mockk()
    private val nonceDecryptor: AuthNonceDecryptor = mockk()
    private val appInfoProvider: AppInfoProvider = mockk(relaxed = true)
    private val signedRequestPayload = SignedRequestPayload(appInfoProvider)
    private val errorConverter = AuthErrorConverter()
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochSeconds(1_700_000_000)
    }

    private lateinit var refresher: DefaultSessionTokenRefresher

    @BeforeEach
    fun setup() {
        clearMocks(authApi, store, deviceKeyManager, nonceDecryptor)
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        refresher = DefaultSessionTokenRefresher(
            authApi = authApi,
            store = store,
            deviceKeyManager = deviceKeyManager,
            nonceDecryptor = nonceDecryptor,
            signedRequestPayload = signedRequestPayload,
            errorConverter = errorConverter,
            clock = fixedClock,
            dispatchers = dispatchers,
        )
    }

    @AfterEach
    fun teardown() = unmockkAll()

    @Test
    fun `refresh hits refresh endpoint when refresh token valid`() = runTest {
        val stored = SessionTokens(
            accessToken = "old-access",
            accessTokenExpiresAt = fixedClock.now().plus(60),
            refreshToken = "rt-1",
            refreshTokenExpiresAt = fixedClock.now().plus(3600),
            walletIds = listOf("w1"),
        )
        coEvery { store.get() } returns Some(stored)
        coEvery { authApi.refresh(RefreshApiRequest("rt-1")) } returns ApiResponse.Success(
            data = TokenApiResponse(
                accessToken = "new-access",
                accessTokenExpiresAt = "2024-01-01T00:00:00Z",
                refreshToken = "rt-2",
                refreshTokenExpiresAt = "2024-02-01T00:00:00Z",
                walletIds = listOf("w1", "w2"),
            ),
        )

        val result = refresher.refresh()

        assertThat(result.isRight()).isTrue()
        val tokens = result.getOrNull()!!
        assertThat(tokens.accessToken).isEqualTo("new-access")
        assertThat(tokens.refreshToken).isEqualTo("rt-2")
        assertThat(tokens.walletIds).containsExactly("w1", "w2")
        coVerify { store.save(tokens) }
    }

    @Test
    fun `refresh falls back to authenticate when refresh returns 401`() = runTest {
        val stored = SessionTokens(
            accessToken = "old-access",
            accessTokenExpiresAt = fixedClock.now().plus(60),
            refreshToken = "rt-1",
            refreshTokenExpiresAt = fixedClock.now().plus(3600),
            walletIds = listOf("w1"),
        )
        coEvery { store.get() } returns Some(stored)
        @Suppress("UNCHECKED_CAST")
        coEvery { authApi.refresh(any()) } returns ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.UNAUTHORIZED,
                message = "revoked",
                errorBody = null,
            ),
        ) as ApiResponse<TokenApiResponse>
        stubAuthenticateHappyPath()

        val result = refresher.refresh()

        assertThat(result.isRight()).isTrue()
        assertThat(result.getOrNull()?.accessToken).isEqualTo("post-auth-access")
        coVerify { authApi.refresh(any()) }
        coVerify { authApi.requestAuthNonce(any()) }
        coVerify { authApi.authenticate(any()) }
    }

    @Test
    fun `refresh clears store when authenticate returns 403`() = runTest {
        coEvery { store.get() } returns None

        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestAuthNonce(any()) } returns ApiResponse.Success(
            data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
        )
        coEvery { nonceDecryptor.decryptNonce("abc") } returns "nonce-decrypted"
        coEvery { deviceKeyManager.sign(any()) } returns ByteArray(64)
        @Suppress("UNCHECKED_CAST")
        coEvery { authApi.authenticate(any<AuthApiRequest>()) } returns ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.FORBIDDEN,
                message = "RED",
                errorBody = null,
            ),
        ) as ApiResponse<TokenApiResponse>

        val result = refresher.refresh()

        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isEqualTo(SessionRefreshError.SessionRevoked)
        coVerify { store.clear() }
    }

    @Test
    fun `concurrent callers share a single refresh round-trip — both get same result`() = runTest {
        val stored = SessionTokens(
            accessToken = "old-access",
            accessTokenExpiresAt = fixedClock.now().plus(60),
            refreshToken = "rt-1",
            refreshTokenExpiresAt = fixedClock.now().plus(3600),
            walletIds = listOf("w1"),
        )
        coEvery { store.get() } returns Some(stored)

        // Gate the network call so both callers reach the single-flight check before the owner
        // completes the in-flight Deferred.
        val networkGate = CompletableDeferred<Unit>()
        coEvery { authApi.refresh(RefreshApiRequest("rt-1")) } coAnswers {
            networkGate.await()
            ApiResponse.Success(
                data = TokenApiResponse(
                    accessToken = "new-access",
                    accessTokenExpiresAt = "2024-01-01T00:00:00Z",
                    refreshToken = "rt-2",
                    refreshTokenExpiresAt = "2024-02-01T00:00:00Z",
                    walletIds = listOf("w1"),
                ),
            )
        }

        val a = async(start = CoroutineStart.UNDISPATCHED) { refresher.refresh() }
        val b = async(start = CoroutineStart.UNDISPATCHED) { refresher.refresh() }

        networkGate.complete(Unit)

        val resultA = a.await()
        val resultB = b.await()

        assertThat(resultA).isEqualTo(resultB)
        assertThat(resultA.getOrNull()?.accessToken).isEqualTo("new-access")
        coVerify(exactly = 1) { authApi.refresh(any()) }
    }

    @Test
    fun `concurrent callers share a transient failure — only one network call is made`() = runTest {
        val stored = SessionTokens(
            accessToken = "old-access",
            accessTokenExpiresAt = fixedClock.now().plus(60),
            refreshToken = "rt-1",
            refreshTokenExpiresAt = fixedClock.now().plus(3600),
            walletIds = listOf("w1"),
        )
        coEvery { store.get() } returns Some(stored)

        val networkGate = CompletableDeferred<Unit>()
        @Suppress("UNCHECKED_CAST")
        coEvery { authApi.refresh(any()) } coAnswers {
            networkGate.await()
            ApiResponse.Error(
                cause = ApiResponseError.HttpException(
                    code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
                    message = "boom",
                    errorBody = null,
                ),
            ) as ApiResponse<TokenApiResponse>
        }

        val a = async(start = CoroutineStart.UNDISPATCHED) { refresher.refresh() }
        val b = async(start = CoroutineStart.UNDISPATCHED) { refresher.refresh() }

        networkGate.complete(Unit)

        val resultA = a.await()
        val resultB = b.await()

        // Both waiters receive the same transient failure; the failing network call wasn't repeated
        // — protects against amplifying outages or replaying a possibly-consumed refresh token.
        assertThat(resultA).isEqualTo(resultB)
        assertThat(resultA.leftOrNull()).isInstanceOf(SessionRefreshError.Api::class.java)
        coVerify(exactly = 1) { authApi.refresh(any()) }
    }

    @Test
    fun `concurrent waiters receive the same exception when the owner's refresh throws`() = runTest {
        val stored = SessionTokens(
            accessToken = "old-access",
            accessTokenExpiresAt = fixedClock.now().plus(60),
            refreshToken = "rt-1",
            refreshTokenExpiresAt = fixedClock.now().plus(3600),
            walletIds = listOf("w1"),
        )
        coEvery { store.get() } returns Some(stored)

        val networkGate = CompletableDeferred<Unit>()
        coEvery { authApi.refresh(any()) } coAnswers {
            networkGate.await()
            throw IllegalStateException("boom")
        }

        val a = async(start = CoroutineStart.UNDISPATCHED) { runCatching { refresher.refresh() } }
        val b = async(start = CoroutineStart.UNDISPATCHED) { runCatching { refresher.refresh() } }

        networkGate.complete(Unit)

        val resultA = a.await()
        val resultB = b.await()

        // Both the owner and the waiter receive the same `IllegalStateException` — proves waiters
        // can't suspend forever when the owner throws (deferred is completed exceptionally).
        assertThat(resultA.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(resultB.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        coVerify(exactly = 1) { authApi.refresh(any()) }

        // The inFlight slot must be cleared so the next call can start a fresh attempt.
        coEvery { authApi.refresh(any()) } returns ApiResponse.Success(
            data = TokenApiResponse(
                accessToken = "after-recovery",
                accessTokenExpiresAt = "2024-01-01T00:00:00Z",
                refreshToken = "rt-2",
                refreshTokenExpiresAt = "2024-02-01T00:00:00Z",
                walletIds = listOf("w1"),
            ),
        )
        val recovered = refresher.refresh()
        assertThat(recovered.getOrNull()?.accessToken).isEqualTo("after-recovery")
    }

    @Test
    fun `refresh skips refresh endpoint when refresh token expired`() = runTest {
        val stored = SessionTokens(
            accessToken = "old-access",
            accessTokenExpiresAt = fixedClock.now().minus(60),
            refreshToken = "rt-1",
            refreshTokenExpiresAt = fixedClock.now().minus(1),
            walletIds = listOf("w1"),
        )
        coEvery { store.get() } returns Some(stored)
        stubAuthenticateHappyPath()

        val result = refresher.refresh()

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { authApi.refresh(any()) }
        coVerify { authApi.authenticate(any()) }
    }

    private fun stubAuthenticateHappyPath() {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestAuthNonce(any()) } returns ApiResponse.Success(
            data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
        )
        coEvery { nonceDecryptor.decryptNonce("abc") } returns "nonce-decrypted"
        coEvery { deviceKeyManager.sign(any()) } returns ByteArray(64)
        coEvery { authApi.authenticate(any<AuthApiRequest>()) } returns ApiResponse.Success(
            data = TokenApiResponse(
                accessToken = "post-auth-access",
                accessTokenExpiresAt = "2024-01-01T00:00:00Z",
                refreshToken = "post-auth-rt",
                refreshTokenExpiresAt = "2024-02-01T00:00:00Z",
                walletIds = listOf("w1"),
            ),
        )
    }

    private fun Instant.plus(seconds: Long): Instant = Instant.fromEpochSeconds(epochSeconds + seconds)
    private fun Instant.minus(seconds: Long): Instant = Instant.fromEpochSeconds(epochSeconds - seconds)
}