package com.tangem.data.polymarket

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.data.polymarket.converter.PolymarketEventConverter
import com.tangem.data.polymarket.converter.PolymarketWalletConverter
import com.tangem.data.polymarket.error.PolymarketAuthErrorResolver
import com.tangem.data.polymarket.error.PolymarketWalletErrorResolver
import com.tangem.data.polymarket.signer.Base64UrlCodec
import com.tangem.data.polymarket.signer.PolymarketHmacSigner
import com.tangem.data.polymarket.signer.PolymarketL2HeaderBuilder
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.polymarket.PolymarketApi
import com.tangem.datasource.api.polymarket.clob.PolymarketClobApi
import com.tangem.datasource.api.polymarket.clob.models.PolymarketApiKeyResponse
import com.tangem.datasource.api.polymarket.geo.PolymarketGeoApi
import com.tangem.datasource.api.polymarket.geo.models.PolymarketGeoblockResponse
import com.tangem.datasource.api.polymarket.models.PolymarketWalletApprovalsRequest
import com.tangem.datasource.api.polymarket.models.PolymarketWalletDeployRequest
import com.tangem.datasource.api.polymarket.models.PolymarketWalletOperationResponse
import com.tangem.datasource.api.polymarket.models.PolymarketWalletStatusResponse
import com.tangem.datasource.api.polymarket.relayer.PolymarketRelayerApi
import com.tangem.datasource.api.polymarket.relayer.models.PolymarketNonceResponse
import com.tangem.domain.core.error.DataError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64 as JavaBase64

internal class DefaultPolymarketRepositoryTest {

    private val api: PolymarketApi = mockk()
    private val geoApi: PolymarketGeoApi = mockk()
    private val relayerApi: PolymarketRelayerApi = mockk()
    private val clobApi: PolymarketClobApi = mockk()
    private val eventConverter: PolymarketEventConverter = mockk()
    private val walletConverter = PolymarketWalletConverter()
    private val walletErrorResolver = PolymarketWalletErrorResolver(Moshi.Builder().build())
    private val authErrorResolver = PolymarketAuthErrorResolver()
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val jvmCodec = object : Base64UrlCodec {
        override fun decode(value: String): ByteArray = JavaBase64.getUrlDecoder().decode(value)
        override fun encode(bytes: ByteArray): String = JavaBase64.getUrlEncoder().encodeToString(bytes)
    }
    private val l2HeaderBuilder = PolymarketL2HeaderBuilder(PolymarketHmacSigner(jvmCodec))

    private val repository = DefaultPolymarketRepository(
        polymarketApi = api,
        geoApi = geoApi,
        relayerApi = relayerApi,
        clobApi = clobApi,
        eventConverter = eventConverter,
        walletConverter = walletConverter,
        walletErrorResolver = walletErrorResolver,
        authErrorResolver = authErrorResolver,
        l2HeaderBuilder = l2HeaderBuilder,
        dispatchers = dispatchers,
    )

    @Test
    fun `GIVEN stored wallet WHEN getWalletStatus THEN maps to domain state`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } returns
            ApiResponse.Success(PolymarketWalletStatusResponse(depositWalletAddress = DW, status = "DEPLOYED"))

        // Act
        val result = repository.getWalletStatus(OWNER)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletState(DW, PolymarketWalletStatus.DEPLOYED).right())
    }

    @Test
    fun `GIVEN unknown status string WHEN getWalletStatus THEN maps to UNKNOWN`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } returns
            ApiResponse.Success(PolymarketWalletStatusResponse(depositWalletAddress = null, status = "SOME_FUTURE_STATE"))

        // Act
        val result = repository.getWalletStatus(OWNER)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletState(null, PolymarketWalletStatus.UNKNOWN).right())
    }

    @Test
    fun `GIVEN 409 WHEN getWalletStatus THEN WalletNotDeployed error (delegated to resolver)`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } returns httpError(Code.CONFLICT, body = null)

        // Act
        val result = repository.getWalletStatus(OWNER)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletError.WalletNotDeployed.left())
    }

    @Test
    fun `GIVEN network exception WHEN getWalletStatus THEN Network error`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } returns networkError()

        // Act
        val result = repository.getWalletStatus(OWNER)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletError.Network.left())
    }

    @Test
    fun `GIVEN cancellation WHEN getWalletStatus THEN it propagates and is not mapped to a domain error`() = runTest {
        // Arrange
        coEvery { api.getWalletStatus(OWNER) } throws CancellationException("cancelled")

        // Act
        val thrown = runCatching { repository.getWalletStatus(OWNER) }.exceptionOrNull()

        // Assert
        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun `GIVEN accepted WHEN deployWallet THEN maps operation status`() = runTest {
        // Arrange
        val request = slot<PolymarketWalletDeployRequest>()
        coEvery { api.deployWallet(capture(request)) } returns
            ApiResponse.Success(PolymarketWalletOperationResponse(status = "DEPLOYMENT_IN_PROGRESS"))

        // Act
        val result = repository.deployWallet(
            ownerAddress = OWNER,
            userWalletId = UserWalletId("0011"),
            depositWalletAddress = "0xDeF0000000000000000000000000000000000002",
        )

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS.right())
        assertThat(request.captured.ownerAddress).isEqualTo(OWNER)
        assertThat(request.captured.walletId).isEqualTo("0011")
        assertThat(request.captured.depositWalletAddress).isEqualTo("0xDeF0000000000000000000000000000000000002")
    }

    @Test
    fun `GIVEN signed batch WHEN submitApprovals THEN sends converted request and maps status`() = runTest {
        // Arrange
        val requestSlot = slot<PolymarketWalletApprovalsRequest>()
        coEvery { api.submitApprovals(capture(requestSlot)) } returns
            ApiResponse.Success(PolymarketWalletOperationResponse(status = "READY_TO_TRADE"))
        val batch = PolymarketApprovalsBatch(
            ownerAddress = OWNER,
            depositWalletAddress = DW,
            nonce = "0",
            deadline = "1752346200",
            calls = listOf(PolymarketApprovalCall(target = "0xToken", value = "0", data = "0x095ea7b3")),
            signature = "0xbatchSig",
        )

        // Act
        val result = repository.submitApprovals(batch)

        // Assert
        assertThat(result).isEqualTo(PolymarketWalletStatus.READY_TO_TRADE.right())
        val sent = requestSlot.captured
        assertThat(sent.ownerAddress).isEqualTo(OWNER)
        assertThat(sent.depositWalletAddress).isEqualTo(DW)
        assertThat(sent.nonce).isEqualTo("0")
        assertThat(sent.calls).hasSize(1)
        assertThat(sent.calls.first().data).isEqualTo("0x095ea7b3")
        assertThat(sent.signature).isEqualTo("0xbatchSig")
    }

    @Test
    fun `GIVEN geoblock true WHEN checkGeoblock THEN returns right true`() = runTest {
        // Arrange
        coEvery { geoApi.getGeoblock() } returns ApiResponse.Success(PolymarketGeoblockResponse(blocked = true))

        // Act
        val actual = repository.checkGeoblock()

        // Assert
        assertThat(actual).isEqualTo(true.right())
    }

    @Test
    fun `GIVEN geo api throws WHEN checkGeoblock THEN returns left no internet`() = runTest {
        // Arrange
        coEvery { geoApi.getGeoblock() } returns networkError()

        // Act
        val actual = repository.checkGeoblock()

        // Assert
        assertThat(actual).isEqualTo(DataError.NetworkError.NoInternetConnection.left())
    }

    @Test
    fun `GIVEN nonce string WHEN getRelayerNonce THEN returns right BigInteger`() = runTest {
        // Arrange
        coEvery { relayerApi.getNonce(OWNER, "WALLET") } returns ApiResponse.Success(PolymarketNonceResponse(nonce = "7"))

        // Act
        val actual = repository.getRelayerNonce(OWNER)

        // Assert
        assertThat(actual).isEqualTo(BigInteger("7").right())
    }

    @Test
    fun `GIVEN relayer throws WHEN getRelayerNonce THEN returns left no internet`() = runTest {
        // Arrange
        coEvery { relayerApi.getNonce(OWNER, "WALLET") } returns networkError()

        // Act
        val actual = repository.getRelayerNonce(OWNER)

        // Assert
        assertThat(actual).isEqualTo(DataError.NetworkError.NoInternetConnection.left())
    }

    @Test
    fun `GIVEN malformed nonce WHEN getRelayerNonce THEN returns left no internet`() = runTest {
        // Arrange
        coEvery { relayerApi.getNonce(OWNER, "WALLET") } returns
            ApiResponse.Success(PolymarketNonceResponse(nonce = "not-a-number"))

        // Act
        val actual = repository.getRelayerNonce(OWNER)

        // Assert
        assertThat(actual).isEqualTo(DataError.NetworkError.NoInternetConnection.left())
    }

    @Test
    fun `GIVEN api key response WHEN deriveApiCredentials THEN returns right credentials`() = runTest {
        // Arrange
        coEvery { clobApi.deriveApiKey(HEADERS.toMap()) } returns
            ApiResponse.Success(PolymarketApiKeyResponse(apiKey = "k", secret = "s", passphrase = "p"))

        // Act
        val actual = repository.deriveApiCredentials(HEADERS)

        // Assert
        assertThat(actual).isEqualTo(PolymarketApiCredentials(apiKey = "k", secret = "s", passphrase = "p").right())
    }

    @Test
    fun `GIVEN 404 WHEN deriveApiCredentials THEN returns left KeyNotFound`() = runTest {
        // Arrange
        coEvery { clobApi.deriveApiKey(HEADERS.toMap()) } returns httpError(Code.NOT_FOUND, body = null)

        // Act
        val actual = repository.deriveApiCredentials(HEADERS)

        // Assert
        assertThat(actual).isEqualTo(PolymarketAuthError.KeyNotFound.left())
    }

    @Test
    fun `GIVEN api key response WHEN createApiCredentials THEN returns right credentials`() = runTest {
        // Arrange
        coEvery { clobApi.createApiKey(HEADERS.toMap()) } returns
            ApiResponse.Success(PolymarketApiKeyResponse(apiKey = "k", secret = "s", passphrase = "p"))

        // Act
        val actual = repository.createApiCredentials(HEADERS)

        // Assert
        assertThat(actual).isEqualTo(PolymarketApiCredentials(apiKey = "k", secret = "s", passphrase = "p").right())
    }

    @Test
    fun `GIVEN credentials WHEN syncBalanceAllowance THEN signs the path without query and sends the fixed params`() =
        runTest {
            // Arrange
            val headers = slot<Map<String, String>>()
            val assetType = slot<String>()
            val signatureType = slot<Int>()
            coEvery {
                clobApi.updateBalanceAllowance(capture(headers), capture(assetType), capture(signatureType))
            } returns ApiResponse.Success(Unit)

            // Act
            val result = repository.syncBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

            // Assert
            assertThat(result).isEqualTo(Unit.right())
            assertThat(assetType.captured).isEqualTo("COLLATERAL")
            assertThat(signatureType.captured).isEqualTo(3)
            assertThat(headers.captured["POLY_ADDRESS"]).isEqualTo(OWNER)
            assertThat(headers.captured["POLY_API_KEY"]).isEqualTo(SYNC_CREDENTIALS.apiKey)
            assertThat(headers.captured["POLY_PASSPHRASE"]).isEqualTo(SYNC_CREDENTIALS.passphrase)
            val timestamp = headers.captured.getValue("POLY_TIMESTAMP")
            assertThat(headers.captured["POLY_SIGNATURE"])
                .isEqualTo(hmac(timestamp + "GET" + "/balance-allowance/update"))
        }

    @Test
    fun `GIVEN a 401 WHEN syncBalanceAllowance THEN maps to InvalidSignature`() = runTest {
        // Arrange
        coEvery { clobApi.updateBalanceAllowance(any(), any(), any()) } returns httpError(Code.UNAUTHORIZED, body = null)

        // Act
        val result = repository.syncBalanceAllowance(ownerAddress = OWNER, credentials = SYNC_CREDENTIALS)

        // Assert
        assertThat(result).isEqualTo(PolymarketAuthError.InvalidSignature.left())
    }

    private fun hmac(message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(JavaBase64.getUrlDecoder().decode(SYNC_CREDENTIALS.secret), "HmacSHA256"))
        return JavaBase64.getUrlEncoder().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> httpError(code: Code, body: String?): ApiResponse<T> =
        ApiResponse.Error(
            ApiResponseError.HttpException(code = code, message = "error", errorBody = body),
        ) as ApiResponse<T>

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> networkError(): ApiResponse<T> =
        ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<T>

    private companion object {
        const val OWNER = "0xAbC0000000000000000000000000000000000001"
        const val DW = "0xDEf0000000000000000000000000000000000002"
        val HEADERS = PolymarketL1Headers(address = "0xabc", signature = "0xsig", timestamp = "1700", nonce = "0")
        val SYNC_CREDENTIALS = PolymarketApiCredentials(
            apiKey = "k",
            secret = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
            passphrase = "p",
        )
    }
}