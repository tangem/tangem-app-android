package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.visa.error.VisaApiError
import org.joda.time.DateTime
import java.math.BigDecimal
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
            .getEnvironmentConfig(ApiConfig.ID.TangemPay)
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
        months: Int,
    ): Either<VisaApiError, CashbackHistory> {
        if (isMockMode) return MOCK_HISTORY.copy(months = MOCK_HISTORY.months.takeLast(months)).right()
        return real.getCashbackHistory(userWalletId, months)
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
                pendingAmount = BigDecimal("13.65"),
                currency = "USD",
                payoutCurrency = "USDC",
                payoutNetwork = "Polygon",
                period = TangemPayCashback.Period(
                    year = 2026,
                    month = 6,
                    payoutStart = DateTime.parse("2026-07-02"),
                    payoutEnd = DateTime.parse("2026-07-05"),
                ),
            ),
        )

        val MOCK_PROMOTIONS = CashbackPromotions(
            cardTiers = listOf(
                CashbackPromotions.CardTier(
                    tier = "basic",
                    label = "Basic cards",
                    scope = "All purchases",
                    minTransactionAmount = BigDecimal("30"),
                    monthlyCapAmount = BigDecimal("100"),
                ),
                CashbackPromotions.CardTier(
                    tier = "plus",
                    label = "Plus cards",
                    scope = "All purchases",
                    minTransactionAmount = BigDecimal("30"),
                    monthlyCapAmount = BigDecimal("300"),
                ),
            ),
            additionalCashback = listOf(
                CashbackPromotions.AdditionalCashback(
                    id = "promo-permanent",
                    name = "Groceries increase",
                    description = "+1% cashback for groceries stores",
                    isPermanent = true,
                    endDate = null,
                ),
                CashbackPromotions.AdditionalCashback(
                    id = "promo-groceries-2026",
                    name = "Groceries increase",
                    description = "+1% cashback for groceries stores. Max \$10/month",
                    isPermanent = false,
                    endDate = DateTime.parse("2026-09-26"),
                ),
                CashbackPromotions.AdditionalCashback(
                    id = "promo-cashback-2026",
                    name = "Cashback increase",
                    description = "+2% cashback for groceries stores. Max \$10/month",
                    isPermanent = false,
                    endDate = DateTime.parse("2026-09-26"),
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
            currency = "USD",
            months = listOf(
                CashbackHistory.MonthlyCashback(year = 2026, month = 2, confirmedAmount = BigDecimal("12.02")),
                CashbackHistory.MonthlyCashback(year = 2026, month = 3, confirmedAmount = BigDecimal("44.22")),
                CashbackHistory.MonthlyCashback(year = 2026, month = 4, confirmedAmount = BigDecimal("38.52")),
                CashbackHistory.MonthlyCashback(year = 2026, month = 5, confirmedAmount = BigDecimal("26.10")),
                CashbackHistory.MonthlyCashback(year = 2026, month = 6, confirmedAmount = BigDecimal("22.54")),
            ),
        )
    }
}