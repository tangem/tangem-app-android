package com.tangem.features.earn.yield.impl

import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.features.earn.product.api.product.EarnRateEntry
import com.tangem.features.earn.product.api.product.EarnRateSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Yield Mode of tokens: a rate and a click route without a product of its own. */
internal class YieldModeRateSource @Inject constructor(
    private val accountStatusListSupplier: SingleAccountStatusListSupplier,
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    private val mapper: YieldModeRateMapper,
) : EarnRateSource {

    override val id: EarnProductId = EarnProductId("yield_mode")

    override val rank: Int = 3

    override fun rates(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnRateEntry>> = combine(
        flow = accountStatusListSupplier(userWalletId).map { it.flattenCurrencies() },
        flow2 = yieldSupplyApyFlowUseCase(),
    ) { statuses, apyByKey ->
        statuses.mapNotNull { status ->
            mapper.entry(userWalletId, status, apyByKey)?.let { entry -> status.currency.id to entry }
        }.toMap()
    }
}