package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CashbackDocument
import com.tangem.domain.tangempay.model.CashbackHistory
import com.tangem.domain.tangempay.model.CashbackPromotions
import com.tangem.domain.tangempay.model.CashbackSummary
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.TangemPayTxHistoryItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository for the cashback endpoints (`GET /v1/customer/cashback/...`).
 */
interface CashbackRepository {

    /**
     * Loads the cashback summary for the customer of [userWalletId] and caches it for [observeCashbackSummary]: a summary
     * worth showing replaces the cached one, a disabled or unrecognized one clears it, a failed request leaves it as is.
     */
    suspend fun getCashbackSummary(userWalletId: UserWalletId): Either<TangemPayApiError, CashbackSummary>

    /** Cached cashback summary of the customer of [userWalletId]; `null` while nothing is cached. */
    fun observeCashbackSummary(userWalletId: UserWalletId): Flow<CashbackSummary?>

    suspend fun getCashbackPromotions(userWalletId: UserWalletId): Either<TangemPayApiError, CashbackPromotions>

    suspend fun getCashbackAccrualDocs(userWalletId: UserWalletId): Either<TangemPayApiError, List<CashbackDocument>>

    /**
     * Loads the confirmed cashback history for the customer of [userWalletId], grouped by month.
     *
     * @param monthsNumber number of calendar months to return, counting back from and including the current
     *                     month.
     */
    suspend fun getCashbackHistory(
        userWalletId: UserWalletId,
        monthsNumber: Int,
    ): Either<TangemPayApiError, CashbackHistory>

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
    ): Either<TangemPayApiError, TangemPayTxHistoryItem.Cashback?>

    /** Whether the "Cashback deactivated" banner was permanently dismissed for [userWalletId]. */
    suspend fun isDeactivationBannerDismissed(userWalletId: UserWalletId): Boolean

    /** Permanently dismisses the "Cashback deactivated" banner for [userWalletId]. */
    suspend fun setDeactivationBannerDismissed(userWalletId: UserWalletId)
}