package com.tangem.data.tangempay.repository

import arrow.core.Either
import com.tangem.spend.datasource.tangempay.TangemPayApi
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.KycStartInfo
import com.tangem.domain.tangempay.repository.KycRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import javax.inject.Inject

internal class DefaultKycRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : KycRepository {

    override suspend fun getKycStartInfo(userWalletId: UserWalletId): Either<TangemPayApiError, KycStartInfo> {
        return requestHelper.performRequest(
            userWalletId,
        ) { authHeader -> tangemPayApi.getKycAccess(authHeader = authHeader) }
            .map { KycStartInfo(token = it.result.token, locale = it.result.locale) }
    }
}