package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.util.CashbackSummaryConverter
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.visa.error.VisaApiError
import javax.inject.Inject

internal class DefaultCashbackRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val storage: TangemPayStorage,
) : CashbackRepository {

    override suspend fun getCashbackSummary(userWalletId: UserWalletId): Either<VisaApiError, CashbackSummary> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCashbackSummary(authHeader = authHeader)
        }.map(CashbackSummaryConverter::convert)
    }

    override suspend fun isDeactivationBannerDismissed(userWalletId: UserWalletId): Boolean {
        val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
        return storage.getCashbackDeactivationDismissed(customerWalletAddress)
    }

    override suspend fun setDeactivationBannerDismissed(userWalletId: UserWalletId) {
        val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
        storage.storeCashbackDeactivationDismissed(customerWalletAddress = customerWalletAddress, isDismissed = true)
    }
}