package com.tangem.data.pay.repository

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultKycRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val requestHelper: TangemPayRequestPerformer,
) : KycRepository {

    override suspend fun getKycStartInfo() = withContext(dispatchers.io) {
        requestHelper.request { authHeader ->
            tangemPayApi.getKycAccess(authHeader = authHeader).getOrThrow().result
        }.map {
            KycStartInfo(token = it.token, locale = it.locale)
        }
    }
}