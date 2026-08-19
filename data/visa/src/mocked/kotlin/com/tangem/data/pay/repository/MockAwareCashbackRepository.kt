package com.tangem.data.pay.repository

import com.tangem.spend.datasource.config.TangemPay

import arrow.core.Either
import arrow.core.right
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.ExclusionReason
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.Status
import org.joda.time.DateTime
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

/** In MOCK env returns canned cashback data; otherwise delegates to [DefaultCashbackRepository]. */
@Singleton
internal class MockAwareCashbackRepository @Inject constructor(
    private val real: DefaultCashbackRepository,
    private val apiConfigsManager: ApiConfigsManager,
) : CashbackRepository {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    override suspend fun getCashbackSummary(userWalletId: UserWalletId): Either<VisaApiError, CashbackSummary> {
        if (isMockMode) return MOCK_SUMMARY.right()
        return real.getCashbackSummary(userWalletId)
    }

    override suspend fun getCashbackPromotions(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, CashbackPromotions> {
        if (isMockMode) return MOCK_PROMOTIONS.right()
        return real.getCashbackPromotions(userWalletId)
    }

    override suspend fun getCashbackAccrualDocs(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, List<CashbackDocument>> {
        if (isMockMode) return MOCK_DOCS.right()
        return real.getCashbackAccrualDocs(userWalletId)
    }

    override suspend fun getCashbackHistory(
        userWalletId: UserWalletId,
        monthsNumber: Int,
    ): Either<VisaApiError, CashbackHistory> {
        if (isMockMode) return MOCK_HISTORY.copy(months = MOCK_HISTORY.months.takeLast(monthsNumber)).right()
        return real.getCashbackHistory(userWalletId, monthsNumber)
    }

    override suspend fun getCashbackDetails(
        userWalletId: UserWalletId,
        transactionId: String,
    ): Either<VisaApiError, TangemPayTxHistoryItem.Cashback?> {
        if (isMockMode) return mockCashbackDetails(transactionId).right()
        return real.getCashbackDetails(userWalletId, transactionId)
    }

    override suspend fun isDeactivationBannerDismissed(userWalletId: UserWalletId): Boolean =
        real.isDeactivationBannerDismissed(userWalletId)

    override suspend fun setDeactivationBannerDismissed(userWalletId: UserWalletId) =
        real.setDeactivationBannerDismissed(userWalletId)

    private companion object {
        val MOCK_SUMMARY = CashbackSummary.Enabled(
            displayMode = CashbackDisplayMode.FULL,
            cashback = TangemPayCashback(
                confirmedAmount = BigDecimal("22.54"),
                totalEarnedAmount = BigDecimal("132.15"),
                currency = "USD",
                period = TangemPayCashback.Period(
                    year = 2026,
                    month = 6,
                    payoutStart = DateTime.parse("2026-07-02"),
                    payoutEnd = DateTime.parse("2026-07-05"),
                ),
                previousPayout = TangemPayCashback.PreviousPayout(
                    endDate = DateTime.parse("2026-06-05"),
                    amount = BigDecimal("18.00"),
                ),
            ),
        )

        val MOCK_PROMOTIONS = CashbackPromotions(
            cards = listOf(
                CashbackPromotions.CardPromotion(
                    cardType = "basic",
                    title = "Basic Card",
                    cashbackRate = BigDecimal("1.0"),
                    minTransactionAmount = BigDecimal("30"),
                    promotionId = "2553142c-19b2-4843-b39d-7882e0b8a6e7",
                ),
                CashbackPromotions.CardPromotion(
                    cardType = "plus",
                    title = "Plus Card",
                    cashbackRate = BigDecimal("2.0"),
                    minTransactionAmount = BigDecimal("30"),
                    promotionId = "997d42ca-892c-4918-92ef-f852d1feb2c4",
                ),
            ),
            accountMonthlyCap = CashbackPromotions.MonthlyCap(amount = BigDecimal("300"), currency = "USD"),
            additionalCashback = listOf(
                CashbackPromotions.AdditionalCashback(
                    id = "promo-permanent",
                    cardType = null,
                    name = "Groceries increase",
                    description = "+1% cashback for groceries stores",
                    endDate = null,
                    promoCap = null,
                    minTransactionAmount = null,
                    priority = 99,
                ),
                CashbackPromotions.AdditionalCashback(
                    id = "promo-groceries-2026",
                    cardType = "plus",
                    name = "Groceries increase",
                    description = "+1% cashback for groceries stores. Max \$10/month",
                    endDate = DateTime.parse("2026-09-26"),
                    promoCap = CashbackPromotions.PromoCap(
                        amount = BigDecimal("10"),
                        period = CashbackPromotions.PromoCap.Period.MONTHLY,
                        currency = "USD",
                    ),
                    minTransactionAmount = BigDecimal("30"),
                    priority = 98,
                ),
                CashbackPromotions.AdditionalCashback(
                    id = "promo-cashback-2026",
                    cardType = "basic",
                    name = "Cashback increase",
                    description = null,
                    endDate = DateTime.parse("2026-09-26"),
                    promoCap = CashbackPromotions.PromoCap(
                        amount = BigDecimal("20"),
                        period = CashbackPromotions.PromoCap.Period.MONTHLY,
                        currency = "USD",
                    ),
                    minTransactionAmount = null,
                    priority = 50,
                ),
            ),
        )

        val MOCK_DOCS = listOf(
            CashbackDocument(
                id = "excluded",
                title = "All categories without cashback",
                url = "https://tangem.com/docs/en/tangem-pay-cashback-excluded-mccs.pdf",
            ),
            CashbackDocument(
                id = "terms",
                title = "Full terms of cashback program",
                url = "https://tangem.com/docs/en/tangem-pay-cashback-terms.pdf",
            ),
        )

        val MOCK_HISTORY = CashbackHistory(
            months = listOf(
                mockMonth(month = 2, amount = "12.02"),
                mockMonth(month = 3, amount = "44.22"),
                mockMonth(month = 4, amount = "38.52"),
                mockMonth(month = 5, amount = "26.10"),
                mockMonth(month = 6, amount = "22.54"),
            ),
        )

        private fun mockMonth(month: Int, amount: String) = CashbackHistory.MonthlyCashback(
            year = 2026,
            month = month,
            confirmedAmount = BigDecimal(amount),
            currency = "USD",
        )

        private val USD: Currency = Currency.getInstance("USD")

        /**
         * Per-transaction cashback detail keyed by the mock transaction id (see
         * [MockAwareTangemPayTxHistoryRepository] `mockItems`). Covers every detail-row state.
         */
        fun mockCashbackDetails(transactionId: String): TangemPayTxHistoryItem.Cashback? = when (transactionId) {
            "tx_1" -> details(status = Status.CONFIRMED, amount = "0.63")
            "tx_2" -> details(status = Status.ESTIMATED, amount = "1.70")
            "tx_3" -> details(status = Status.CONFIRMED, amount = "-0.80") // refund
            "tx_4" -> details(status = Status.AWAITING_CALCULATION, amount = null)
            "tx_5" -> details(
                status = Status.EXCLUDED,
                amount = "0.00",
                exclusionReason = ExclusionReason.MCC_EXCLUDED,
            )
            "tx_8" -> details(status = Status.CONFIRMED, amount = "3.00", isCapTrimmed = true)
            "tx_9" -> details(
                status = Status.EXCLUDED,
                amount = "0.00",
                exclusionReason = ExclusionReason.MONTHLY_CAP_REACHED,
            )
            "tx_10" -> details(
                status = Status.EXCLUDED,
                amount = "0.00",
                exclusionReason = ExclusionReason.MERCHANT_COUNTRY_EXCLUDED,
            )
            "tx_11" -> details(status = Status.EXCLUDED, amount = "0.00", exclusionReason = ExclusionReason.BELOW_MIN)
            else -> null
        }

        private fun details(
            status: Status,
            amount: String?,
            exclusionReason: ExclusionReason? = null,
            isCapTrimmed: Boolean = false,
        ): TangemPayTxHistoryItem.Cashback {
            val value = amount?.let(::BigDecimal)
            return TangemPayTxHistoryItem.Cashback(
                status = status,
                amount = value,
                currency = value?.let { USD },
                isCapTrimmed = isCapTrimmed,
                exclusionReason = exclusionReason,
            )
        }
    }
}