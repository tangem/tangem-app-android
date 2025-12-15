package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.domain.visa.error.VisaApiError
import javax.inject.Inject

internal class DefaultKycRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : KycRepository {

    override suspend fun getKycStartInfo(userWalletId: UserWalletId): Either<VisaApiError, KycStartInfo> {
        return requestHelper.performRequest(
            userWalletId,
        ) { authHeader -> tangemPayApi.getKycAccess(authHeader = authHeader) }
            .map { KycStartInfo(token = it.result.token, locale = it.result.locale) }
    }
}