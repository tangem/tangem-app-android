package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.repository.KycRepository
import javax.inject.Inject

private const val TAG = "TangemPay: KycRepository"

internal class DefaultKycRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : KycRepository {

    override suspend fun getKycStartInfo(): Either<UniversalError, KycStartInfo> {
        return requestHelper.runWithErrorLogs(TAG) {
            val result = requestHelper.request { authHeader ->
                tangemPayApi.getKycAccess(authHeader = authHeader)
            }.result

            KycStartInfo(token = result.token, locale = result.locale)
        }
    }
}