package com.tangem.data.polymarket

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.data.polymarket.converter.PolymarketAuthorizeSessionKeyRequestConverter
import com.tangem.data.polymarket.converter.PolymarketRevokeSessionKeyRequestConverter
import com.tangem.data.polymarket.converter.PolymarketSessionKeyConverter
import com.tangem.data.polymarket.error.PolymarketAuthErrorResolver
import com.tangem.data.polymarket.error.PolymarketSessionKeyErrorResolver
import com.tangem.data.polymarket.error.ProblemDetailParser
import com.tangem.data.polymarket.signer.Base64UrlCodec
import com.tangem.data.polymarket.signer.PolymarketHmacSigner
import com.tangem.data.polymarket.signer.PolymarketL2HeaderBuilder
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobApi
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketSessionSignerDto
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketSessionSignersResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketSessionKeyDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSetSessionKeyEnabledRequest
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64 as JavaBase64

internal class DefaultPolymarketSessionKeyRepositoryTest {

    private val api: PolymarketApi = mockk()
    private val clobApi: PolymarketClobApi = mockk()
    private val jvmCodec = object : Base64UrlCodec {
        override fun decode(value: String): ByteArray = JavaBase64.getUrlDecoder().decode(value)
        override fun encode(bytes: ByteArray): String = JavaBase64.getUrlEncoder().encodeToString(bytes)
    }

    private val repository = DefaultPolymarketSessionKeyRepository(
        polymarketApi = api,
        clobApi = clobApi,
        sessionKeyErrorResolver = PolymarketSessionKeyErrorResolver(ProblemDetailParser(Moshi.Builder().build())),
        authErrorResolver = PolymarketAuthErrorResolver(),
        l2HeaderBuilder = PolymarketL2HeaderBuilder(PolymarketHmacSigner(jvmCodec)),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(api, clobApi)
    }

    @Test
    fun `GIVEN an accepted authorization WHEN authorizeSessionKey THEN sends the wallet id and returns the record`() =
        runTest {
            // Arrange
            coEvery { api.authorizeSessionKey(any(), any()) } returns ApiResponse.Success(PENDING_DTO)

            // Act
            val actual = repository.authorizeSessionKey(USER_WALLET_ID, AUTHORIZATION)

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyConverter.convert(PENDING_DTO).right())
            coVerify(exactly = 1) {
                api.authorizeSessionKey(
                    walletId = WALLET_ID,
                    request = PolymarketAuthorizeSessionKeyRequestConverter.convert(AUTHORIZATION),
                )
            }
        }

    @Test
    fun `GIVEN a record with an out of range timestamp WHEN authorizeSessionKey THEN returns Unknown`() = runTest {
        // Arrange
        coEvery { api.authorizeSessionKey(any(), any()) } returns
            ApiResponse.Success(PENDING_DTO.copy(validUntil = Long.MAX_VALUE))

        // Act
        val actual = repository.authorizeSessionKey(USER_WALLET_ID, AUTHORIZATION)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Unknown::class.java)
    }

    @Test
    fun `GIVEN a busy key WHEN authorizeSessionKey THEN returns SessionKeyBusy`() = runTest {
        // Arrange
        coEvery { api.authorizeSessionKey(any(), any()) } returns failure(
            ApiResponseError.HttpException(code = Code.CONFLICT, message = "error", errorBody = problem("SESSION_KEY_BUSY")),
        )

        // Act
        val actual = repository.authorizeSessionKey(USER_WALLET_ID, AUTHORIZATION)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyError.SessionKeyBusy.left())
    }

    @Test
    fun `GIVEN an accepted revocation WHEN revokeSessionKey THEN sends the wallet id and returns the record`() = runTest {
        // Arrange
        val revoking = PENDING_DTO.copy(status = "REVOKING")
        coEvery { api.revokeSessionKey(any(), any()) } returns ApiResponse.Success(revoking)

        // Act
        val actual = repository.revokeSessionKey(USER_WALLET_ID, REVOCATION)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyConverter.convert(revoking).right())
        coVerify(exactly = 1) {
            api.revokeSessionKey(
                walletId = WALLET_ID,
                request = PolymarketRevokeSessionKeyRequestConverter.convert(REVOCATION),
            )
        }
    }

    @Test
    fun `GIVEN a stale nonce WHEN revokeSessionKey THEN returns NonceAlreadyUsed`() = runTest {
        // Arrange
        coEvery { api.revokeSessionKey(any(), any()) } returns failure(
            ApiResponseError.HttpException(
                code = Code.UNPROCESSABLE_ENTITY,
                message = "error",
                errorBody = problem("NONCE_ALREADY_USED"),
            ),
        )

        // Act
        val actual = repository.revokeSessionKey(USER_WALLET_ID, REVOCATION)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyError.NonceAlreadyUsed.left())
    }

    @Test
    fun `GIVEN a toggle WHEN setSessionKeyEnabled THEN puts the flag on the key address`() = runTest {
        // Arrange
        val disabled = PENDING_DTO.copy(isEnabled = false)
        coEvery { api.setSessionKeyEnabled(any(), any(), any()) } returns ApiResponse.Success(disabled)

        // Act
        val actual = repository.setSessionKeyEnabled(USER_WALLET_ID, sessionKeyAddress = KEY, isEnabled = false)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyConverter.convert(disabled).right())
        coVerify(exactly = 1) {
            api.setSessionKeyEnabled(
                walletId = WALLET_ID,
                sessionKeyAddress = KEY,
                request = PolymarketSetSessionKeyEnabledRequest(isEnabled = false),
            )
        }
    }

    @Test
    fun `GIVEN no connection WHEN setSessionKeyEnabled THEN returns Network`() = runTest {
        // Arrange
        coEvery { api.setSessionKeyEnabled(any(), any(), any()) } returns failure(
            ApiResponseError.NetworkException(),
        )

        // Act
        val actual = repository.setSessionKeyEnabled(USER_WALLET_ID, sessionKeyAddress = KEY, isEnabled = true)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyError.Network.left())
    }

    @Test
    fun `GIVEN owner credentials WHEN getSessionSigners THEN signs the path and lowercases the addresses`() = runTest {
        // Arrange
        val headers = slot<Map<String, String>>()
        coEvery { clobApi.getSessionSigners(capture(headers)) } returns ApiResponse.Success(
            PolymarketSessionSignersResponse(
                wallet = DW,
                signers = listOf(
                    PolymarketSessionSignerDto(address = "0x5E55AB", scopes = listOf("CLOB"), validUntil = 1804982400),
                ),
            ),
        )

        // Act
        val actual = repository.getSessionSigners(ownerAddress = OWNER, credentials = CREDENTIALS)

        // Assert
        assertThat(actual).isEqualTo(
            listOf(
                PolymarketSessionSigner(
                    address = "0x5e55ab",
                    scopes = listOf("CLOB"),
                    validUntil = Instant.ofEpochSecond(1804982400),
                ),
            ).right(),
        )
        val timestamp = headers.captured.getValue("POLY_TIMESTAMP")
        assertThat(headers.captured["POLY_ADDRESS"]).isEqualTo(OWNER)
        assertThat(headers.captured["POLY_SIGNATURE"]).isEqualTo(hmac(timestamp + "GET" + "/v1/user/session-signers"))
    }

    @Test
    fun `GIVEN rejected owner credentials WHEN getSessionSigners THEN returns InvalidSignature`() = runTest {
        // Arrange
        coEvery { clobApi.getSessionSigners(any()) } returns failure(
            ApiResponseError.HttpException(code = Code.UNAUTHORIZED, message = "error", errorBody = null),
        )

        // Act
        val actual = repository.getSessionSigners(ownerAddress = OWNER, credentials = CREDENTIALS)

        // Assert
        assertThat(actual).isEqualTo(PolymarketAuthError.InvalidSignature.left())
    }

    @Test
    fun `GIVEN a malformed stored secret WHEN getSessionSigners THEN returns Unknown without calling the CLOB`() =
        runTest {
            // Act
            val actual = repository.getSessionSigners(
                ownerAddress = OWNER,
                credentials = CREDENTIALS.copy(secret = "%%%"),
            )

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(PolymarketAuthError.Unknown::class.java)
            coVerify(exactly = 0) { clobApi.getSessionSigners(any()) }
        }

    private fun hmac(message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(JavaBase64.getUrlDecoder().decode(CREDENTIALS.secret), "HmacSHA256"))
        return JavaBase64.getUrlEncoder().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> failure(error: ApiResponseError): ApiResponse<T> = ApiResponse.Error(error) as ApiResponse<T>

    private fun problem(reason: String) =
        """{"type":"about:blank","title":"t","status":0,"detail":"d","reason":"$reason","instance":"/x"}"""

    private companion object {
        const val WALLET_ID = "7CE25DC32EF792CFC32380007A4172F5B64F67E4F91D37F14B351A76DAFA33DA"
        const val OWNER = "0xAbC0000000000000000000000000000000000001"
        const val DW = "0xdef0000000000000000000000000000000000002"
        const val KEY = "0x5e55000000000000000000000000000000000001"
        val USER_WALLET_ID = UserWalletId(WALLET_ID)
        val CREDENTIALS = PolymarketApiCredentials(
            apiKey = "k",
            secret = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
            passphrase = "p",
        )
        val PENDING_DTO = PolymarketSessionKeyDto(
            address = KEY,
            deviceId = "device",
            deviceName = "Pixel 9",
            isEnabled = true,
            status = "PENDING",
            scopes = listOf("CLOB"),
            validUntil = 1804982400,
            renewAfter = 1804723200,
            createdAt = 1789430280,
        )
        val AUTHORIZATION = PolymarketSessionKeyAuthorization(
            ownerAddress = OWNER,
            depositWalletAddress = DW,
            sessionKeyAddress = KEY,
            deviceId = "device",
            deviceName = "Pixel 9",
            validUntil = "1804982400",
            nonce = "9",
            deadline = "1789431000",
            call = PolymarketApprovalCall(target = DW, value = "0", data = "0x24017fae"),
            signature = "0xsig",
        )
        val REVOCATION = PolymarketSessionKeyRevocation(
            ownerAddress = OWNER,
            depositWalletAddress = DW,
            sessionKeyAddress = KEY,
            nonce = "8",
            deadline = "1789431000",
            call = PolymarketApprovalCall(target = DW, value = "0", data = "0xe63f952f"),
            signature = "0xsig",
        )
    }
}