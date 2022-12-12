package com.tangem.tap.domain.tokens

import com.tangem.common.core.TangemSdkError
import com.tangem.common.services.Result
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.datasource.api.tangemTech.UserTokensResponse
import com.tangem.tap.domain.NoDataError

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

    suspend fun saveUserTokens(userId: String, tokens: UserTokensResponse): Result<Unit> {
        return tangemTechService.putUserTokens(userId, tokens)
    }
}

