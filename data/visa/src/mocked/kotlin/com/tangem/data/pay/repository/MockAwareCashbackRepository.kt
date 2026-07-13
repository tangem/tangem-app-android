package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.visa.error.VisaApiError
import org.joda.time.DateTime
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/** In MOCK env returns a canned cashback summary; otherwise delegates to [DefaultCashbackRepository]. */
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

    private companion object {
        val MOCK_SUMMARY = CashbackSummary.Enabled(
            displayMode = CashbackDisplayMode.FULL,
            cashback = TangemPayCashback(
                confirmedAmount = BigDecimal("22.54"),
                currency = "USD",
                period = TangemPayCashback.Period(
                    year = 2026,
                    month = 6,
                    payoutStart = DateTime.parse("2026-07-02"),
                    payoutEnd = DateTime.parse("2026-07-05"),
                ),
            ),
            pendingAmount = BigDecimal("13.65"),
        )
    }
}