package com.tangem.commands.common.network

import com.tangem.commands.verifycard.CardVerifyAndGetInfo

class TangemService {

    private val tangemApi: TangemApi by lazy {
        createRetrofitInstance(ApiTangem.TANGEM_ENDPOINT).create(TangemApi::class.java)
    }

    suspend fun verifyAndGetInfo(
        cardId: String,
        cardPublicKey: String
    ): Result<CardVerifyAndGetInfo.Response> {
        val requestsBody = CardVerifyAndGetInfo.Request()
        requestsBody.requests =
            listOf(CardVerifyAndGetInfo.Request.Item(cardId, cardPublicKey))

        return performRequest { tangemApi.getCardVerifyAndGetInfo(requestsBody) }
    }

}