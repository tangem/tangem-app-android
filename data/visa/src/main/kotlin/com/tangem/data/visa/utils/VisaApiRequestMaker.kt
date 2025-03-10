package com.tangem.data.visa.utils

import com.tangem.data.visa.converter.AccessCodeDataConverter
import com.tangem.data.visa.model.AccessCodeData
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.visa.TangemVisaAuthApi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.visa.exception.RefreshTokenExpiredException
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.domain.visa.model.getAuthHeader
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.jvm.Throws

typealias VisaAuthorizationHeader = String

internal class VisaApiRequestMaker @Inject constructor(
    private val userWalletsStore: UserWalletsStore,
    private val visaAuthApi: TangemVisaAuthApi,
    private val accessCodeDataConverter: AccessCodeDataConverter,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) {
    suspend fun <T : Any> request(
        userWalletId: UserWalletId,
        requestBlock: suspend (header: VisaAuthorizationHeader, accessCodeData: AccessCodeData) -> ApiResponse<T>,
    ): T = withContext(dispatcherProvider.io) {
        val authTokens = getAuthTokens(userWalletId)
        val authHeader = authTokens.getAuthHeader()
        val accessCodeData = accessCodeDataConverter.convert(authTokens)

        runCatching {
            requestBlock(authHeader, accessCodeData).getOrThrow()
        }.getOrElse { responseError ->
            if (responseError !is ApiResponseError.HttpException ||
                responseError.code != ApiResponseError.HttpException.Code.UNAUTHORIZED
            ) {
                throw responseError
            }

            val newTokens = runCatching {
                refreshAccessTokens(authTokens.refreshToken)
            }.getOrElse {
                if (it is ApiResponseError.HttpException &&
                    it.code == ApiResponseError.HttpException.Code.UNAUTHORIZED
                ) {
                    userWalletsStore.update(userWalletId) { userWallet ->
                        userWallet.copy(
                            scanResponse = userWallet.scanResponse.copy(
                                visaCardActivationStatus = VisaCardActivationStatus.RefreshTokenExpired,
                            ),
                        )
                    }
                }
                throw RefreshTokenExpiredException()
            }

            userWalletsStore.update(userWalletId) { userWallet ->
                userWallet.copy(
                    scanResponse = userWallet.scanResponse.copy(
                        visaCardActivationStatus = VisaCardActivationStatus.Activated(
                            visaAuthTokens = newTokens,
                        ),
                    ),
                )
            }

            val newAuthHeader = newTokens.getAuthHeader()
            val newAccessCodeData = accessCodeDataConverter.convert(newTokens)

            requestBlock(newAuthHeader, newAccessCodeData).getOrThrow()
        }
    }

    private suspend fun refreshAccessTokens(refreshToken: VisaAuthTokens.RefreshToken): VisaAuthTokens {
        val result = visaAuthApi.refreshAccessToken(refreshToken.value).getOrThrow()

        return VisaAuthTokens(
            accessToken = result.accessToken,
            refreshToken = VisaAuthTokens.RefreshToken(result.refreshToken),
        )
    }

    @Throws
    private suspend fun getAuthTokens(userWalletId: UserWalletId): VisaAuthTokens {
        val userWallet = findVisaUserWallet(userWalletId)
        val status = userWallet.scanResponse.visaCardActivationStatus ?: error("Visa card activation status not found")

        if (status is VisaCardActivationStatus.RefreshTokenExpired) {
            throw RefreshTokenExpiredException()
        }

        return (status as? VisaCardActivationStatus.Activated)?.visaAuthTokens ?: error("Visa card is not activated")
    }

    private suspend fun findVisaUserWallet(userWalletId: UserWalletId): UserWallet {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "No user wallet found: $userWalletId"
        }
        if (!userWallet.scanResponse.cardTypesResolver.isVisaWallet()) {
            error("VISA wallet required: $userWalletId")
        }

        return userWallet
    }
}