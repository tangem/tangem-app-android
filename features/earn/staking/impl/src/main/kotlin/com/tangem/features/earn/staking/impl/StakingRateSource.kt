package com.tangem.features.earn.staking.impl

import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.features.earn.product.api.product.EarnRateEntry
import com.tangem.features.earn.product.api.product.EarnRateSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Staking through StakeKit: a rate and a click route without a product of its own. ETH with the P2P pool is a product. */
internal class StakingRateSource @Inject constructor(
    private val accountStatusListSupplier: SingleAccountStatusListSupplier,
    private val stakingRepository: StakingRepository,
    private val mapper: StakingRateMapper,
) : EarnRateSource {

    override val id: EarnProductId = EarnProductId("staking_stakekit")

    override val rank: Int = 1

    override fun rates(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnRateEntry>> {
        val stakeKitStatuses = accountStatusListSupplier(userWalletId)
            .map { statuses -> statuses.flattenCurrencies().filter { it.isStakeKitCurrency() } }

        return combine(stakeKitStatuses, availabilities(userWalletId, stakeKitStatuses)) { statuses, availabilities ->
            statuses.mapNotNull { status ->
                val availability = availabilities[status.currency.id] ?: return@mapNotNull null
                mapper.entry(userWalletId, status, availability)?.let { entry -> status.currency.id to entry }
            }.toMap()
        }
    }

    /** Availability does not depend on balances, so it is resubscribed only when the set of currencies changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun availabilities(
        userWalletId: UserWalletId,
        stakeKitStatuses: Flow<List<CryptoCurrencyStatus>>,
    ): Flow<Map<CryptoCurrency.ID, StakingAvailability>> = stakeKitStatuses
        .map { statuses -> statuses.map { it.currency } }
        .distinctUntilChangedBy { currencies -> currencies.map { it.id } }
        .flatMapLatest { currencies ->
            if (currencies.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(
                    currencies.map { currency ->
                        stakingRepository.getStakingAvailability(userWalletId, currency).map { currency.id to it }
                    },
                ) { entries -> entries.toMap() }
            }
        }

    private fun CryptoCurrencyStatus.isStakeKitCurrency(): Boolean =
        StakingIntegrationID.create(currency.id) is StakingIntegrationID.StakeKit
}