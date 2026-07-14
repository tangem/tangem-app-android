package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.data.pay.util.CashbackAccrualDocsConverter
import com.tangem.data.pay.util.CashbackPromotionsConverter
import com.tangem.data.pay.util.CashbackSummaryConverter
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.SupportedLanguages
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

    override suspend fun getCashbackPromotions(userWalletId: UserWalletId): Either<VisaApiError, CashbackPromotions> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCashbackPromotions(
                authHeader = authHeader,
                language = SupportedLanguages.getCurrentSupportedLanguageCode(),
            )
        }.map(CashbackPromotionsConverter::convert)
    }

    override suspend fun getCashbackAccrualDocs(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, List<CashbackDocument>> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCashbackAccrualDocs(
                authHeader = authHeader,
                language = SupportedLanguages.getCurrentSupportedLanguageCode(),
            )
        }.map(CashbackAccrualDocsConverter::convert)
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