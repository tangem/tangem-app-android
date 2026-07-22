package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayTxHistoryItem

/**
 * Repository for the cashback endpoints (`GET /v1/customer/cashback/...`).
 */
interface CashbackRepository {

    /** Loads the cashback summary for the customer of [userWalletId]. */
    suspend fun getCashbackSummary(userWalletId: UserWalletId): Either<VisaApiError, CashbackSummary>

    suspend fun getCashbackPromotions(userWalletId: UserWalletId): Either<VisaApiError, CashbackPromotions>

    suspend fun getCashbackAccrualDocs(userWalletId: UserWalletId): Either<VisaApiError, List<CashbackDocument>>

    /**
     * Loads the confirmed cashback history for the customer of [userWalletId], grouped by month.
     *
     * @param months number of calendar months to return, counting back from and including the current month.
     */
    suspend fun getCashbackHistory(userWalletId: UserWalletId, months: Int): Either<VisaApiError, CashbackHistory>

    /**
     * Loads the rich per-transaction cashback for [transactionId] (customer of [userWalletId]),
     * shown as the "Cashback" row on the transaction detail screen.
     *
     * Returns `null` when cashback is not applicable to the transaction at all (customer on the
     * cashback ignore list, or a non-spend transaction).
     */
    suspend fun getCashbackDetails(
        userWalletId: UserWalletId,
        transactionId: String,
    ): Either<VisaApiError, TangemPayTxHistoryItem.Cashback?>

    /** Whether the "Cashback deactivated" banner was permanently dismissed for [userWalletId]. */
    suspend fun isDeactivationBannerDismissed(userWalletId: UserWalletId): Boolean

    /** Permanently dismisses the "Cashback deactivated" banner for [userWalletId]. */
    suspend fun setDeactivationBannerDismissed(userWalletId: UserWalletId)
}