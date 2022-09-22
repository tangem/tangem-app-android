package com.tangem.tap.domain.tokens

import com.tangem.common.core.TangemSdkError
import com.tangem.common.services.Result
import com.tangem.network.api.tangemTech.TangemTechService
import com.tangem.network.api.tangemTech.UserTokensResponse
import com.tangem.tap.domain.NoDataError
import com.tangem.tap.features.wallet.models.Currency

class UserTokensNetworkService(private val tangemTechService: TangemTechService) {
    suspend fun getUserTokens(userId: String): Result<UserTokensResponse> {
        return when (val result = tangemTechService.getUserTokens(userId)) {
            is Result.Success -> result
            is Result.Failure -> {
                val error = result.error
                if (error is TangemSdkError.NetworkError && error.customMessage.contains("404")) {
                    return Result.Failure(NoDataError(error.customMessage))
                } else {
                    return result
                }
            }
        }
    }

    suspend fun saveUserTokens(userId: String, tokens: List<Currency>): Result<Unit> {
        val tokensResponse = tokens.map { it.toTokenResponse() }
        val data = UserTokensResponse(tokens = tokensResponse)
        return tangemTechService.putUserTokens(userId, data)
    }
}

