package com.tangem.network.api.paymentology

import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.network.common.MoshiConverter
import com.tangem.network.common.createRetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
[REDACTED_AUTHOR]
 */
class PaymentologyApiService(
    private val logEnabled: Boolean,
) {
    private val api = createRetrofitInstance(
        baseUrl = PaymentologyApi.baseUrl,
        converterFactory = MoshiConverter.createFactory(MoshiConverter.sdkMoshi()),
        logEnabled = logEnabled,
    ).create(PaymentologyApi::class.java)

    suspend fun checkRegistration(
        cardId: String,
        publicKey: ByteArray,
    ): Result<RegistrationResponse> = withContext(Dispatchers.IO) {
        val requestItem = CheckRegistrationRequests.Item(cardId, publicKey.toHexString())
        val request = CheckRegistrationRequests(listOf(requestItem))
        performRequest {
            api.checkRegistration(request)
        }
    }

    suspend fun requestAttestationChallenge(
        cardId: String,
        publicKey: ByteArray,
    ): Result<AttestationResponse> = withContext(Dispatchers.IO) {
        val requestItem = CheckRegistrationRequests.Item(cardId, publicKey.toHexString())
        performRequest {
            api.requestAttestationChallenge(requestItem)
        }
    }

    suspend fun registerWallet(
        request: RegisterWalletRequest,
    ): Result<RegisterWalletResponse> = withContext(Dispatchers.IO) {
        performRequest {
            api.registerWallet(request)
        }
    }

    suspend fun registerKYC(
        request: RegisterKYCRequest,
    ): Result<RegisterWalletResponse> = withContext(Dispatchers.IO) {
        performRequest {
            api.registerKYC(request)
        }
    }

    companion object {
        fun stub(): PaymentologyApiService = PaymentologyApiService(false)
    }
}