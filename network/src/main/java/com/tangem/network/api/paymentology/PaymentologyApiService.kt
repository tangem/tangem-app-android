package com.tangem.network.api.paymentology

import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
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
        logEnabled = logEnabled,
    ).create(PaymentologyApi::class.java)

    suspend fun checkRegistration(
        cardId: String,
        publicKey: ByteArray,
    ): Result<RegistrationResponse.Item> = withContext(Dispatchers.IO) {
        // later convert response to SaltPayRegistrator.State
        val requestItem = CheckRegistrationRequests.Item(cardId, publicKey.toHexString())
        val requests = listOf(requestItem)
        performRequest { api.checkRegistration(CheckRegistrationRequests(requests)) }
    }

    suspend fun requestAttestationChallenge(
        cardId: String,
        publicKey: ByteArray,
    ): Result<AttestationResponse> = withContext(Dispatchers.IO) {
        val requestItem = CheckRegistrationRequests.Item(cardId, publicKey.toHexString())
        performRequest { api.requestAttestationChallenge(requestItem) }
    }

    suspend fun registerWallet(
        request: RegisterWalletRequest,
    ): Result<RegisterWalletResponse> = withContext(Dispatchers.IO) {
        performRequest { api.registerWallet(request) }
    }
}