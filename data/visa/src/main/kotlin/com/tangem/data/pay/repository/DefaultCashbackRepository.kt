package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.store.CashbackSummaryStore
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.data.pay.util.CashbackAccrualDocsConverter
import com.tangem.data.pay.util.CashbackHistoryConverter
import com.tangem.data.pay.util.CashbackPromotionsConverter
import com.tangem.data.pay.util.CashbackSummaryConverter
import com.tangem.data.visa.utils.PayTransactionCashbackConverter
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultCashbackRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val storage: TangemPayStorage,
    private val summaryStore: CashbackSummaryStore,
) : CashbackRepository {

    override suspend fun getCashbackSummary(userWalletId: UserWalletId): Either<VisaApiError, CashbackSummary> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCashbackSummary(authHeader = authHeader)
        }
            .map(CashbackSummaryConverter::convert)
            .onRight { summary -> summaryStore.store(userWalletId, summary) }
    }

    override fun observeCashbackSummary(userWalletId: UserWalletId): Flow<CashbackSummary?> {
        return summaryStore.get(userWalletId)
    }

    override suspend fun getCashbackPromotions(userWalletId: UserWalletId): Either<VisaApiError, CashbackPromotions> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCashbackPromotions(authHeader = authHeader)
        }.map(CashbackPromotionsConverter::convert)
    }

    override suspend fun getCashbackAccrualDocs(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, List<CashbackDocument>> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCashbackAccrualDocs(authHeader = authHeader)
        }.map(CashbackAccrualDocsConverter::convert)
    }

    override suspend fun getCashbackHistory(
        userWalletId: UserWalletId,
        monthsNumber: Int,
    ): Either<VisaApiError, CashbackHistory> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCashbackHistory(authHeader = authHeader, monthsNumber = monthsNumber)
        }.map(CashbackHistoryConverter::convert)
    }

    override suspend fun getCashbackDetails(
        userWalletId: UserWalletId,
        transactionId: String,
    ): Either<VisaApiError, TangemPayTxHistoryItem.Cashback?> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCashbackDetails(authHeader = authHeader, transactionId = transactionId)
        }.map { PayTransactionCashbackConverter.convert(it.result?.cashback) }
    }

    override suspend fun isDeactivationBannerDismissed(userWalletId: UserWalletId): Boolean {
        return runSuspendCatching {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            storage.getCashbackDeactivationDismissed(customerWalletAddress)
        }.getOrElse { throwable ->
            TangemLogger.e("Failed to check if the cashback deactivation banner is dismissed", throwable)
            false
        }
    }

    override suspend fun setDeactivationBannerDismissed(userWalletId: UserWalletId) {
        runSuspendCatching {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            storage.storeCashbackDeactivationDismissed(
                customerWalletAddress = customerWalletAddress,
                isDismissed = true,
            )
        }.onFailure { TangemLogger.e("Failed to persist the cashback deactivation banner dismissal", it) }
    }
}