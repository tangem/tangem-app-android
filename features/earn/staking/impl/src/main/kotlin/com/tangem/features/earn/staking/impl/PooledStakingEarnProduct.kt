package com.tangem.features.earn.staking.impl

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.single.SingleStakingBalanceFetcher
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/** Staking of ETH in the P2P.org pool. It has no shared Earn flow: every click opens the existing staking screen. */
@Suppress("LongParameterList")
internal class PooledStakingEarnProduct @Inject constructor(
    private val accountStatusListSupplier: SingleAccountStatusListSupplier,
    private val stakingRepository: StakingRepository,
    private val p2pEthPoolRepository: P2PEthPoolRepository,
    private val stakingIdFactory: StakingIdFactory,
    private val singleStakingBalanceFetcher: SingleStakingBalanceFetcher,
    private val mapper: PooledStakingMapper,
) : EarnProduct {

    override val id: EarnProductId = POOLED_STAKING_ID

    override val rank: Int = 1

    // Temporary plain text: becomes a Lokalise resource together with the other plain strings of this product.
    override val title: TextReference = stringReference("Pooled staking")

    override val provider: EarnProvider = EarnProvider(
        title = stringReference("P2P.org"),
        icon = ImageReference.Res(R.drawable.ic_p2p_logo),
    )

    override val analytics: EarnProductAnalytics = EarnProductAnalytics(category = "ETH Staking", product = "Staking")

    override val hasLegacyBlock: Boolean = true

    override fun summaries(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnProductSummary>> {
        val poolStatuses = accountStatusListSupplier(userWalletId)
            .map { statuses -> statuses.flattenCurrencies().filter { it.isPoolCurrency() } }

        return combine(poolStatuses, availabilities(userWalletId, poolStatuses)) { statuses, availabilities ->
            statuses.mapNotNull { status ->
                val availability = availabilities[status.currency.id] ?: return@mapNotNull null
                mapper.summary(userWalletId, status, availability)?.let { summary -> status.currency.id to summary }
            }.toMap()
        }
    }

    override fun state(params: EarnProduct.Params): Flow<EarnProductState> = combine(
        flow = accountStatusListSupplier(params.userWalletId).map { statuses ->
            statuses.flattenCurrencies().firstOrNull { it.currency.id == params.currency.id }
        },
        flow2 = stakingRepository.getStakingAvailability(params.userWalletId, params.currency),
    ) { status, availability ->
        status?.let { mapper.state(it, availability) } ?: EarnProductState.Unavailable
    }.onStart { emit(EarnProductState.Loading) }

    override suspend fun apyHistory(params: EarnProduct.Params, period: EarnPeriod): Either<Throwable, EarnApyHistory> =
        EarnApyHistory(points = emptyList(), average = null).right()

    override suspend fun refresh(params: EarnProduct.Params): Either<Throwable, Unit> = either {
        runSuspendCatching {
            p2pEthPoolRepository.fetchVaults()
            p2pEthPoolRepository.fetchVaultLimits()
        }.fold(onSuccess = { Either.Right(Unit) }, onFailure = { Either.Left(it) }).bind()

        val stakingId = stakingIdFactory.create(params.userWalletId, params.currency)
            .mapLeft { error -> IllegalStateException("Can't create the staking id: $error") }
            .bind()

        singleStakingBalanceFetcher(SingleStakingBalanceFetcher.Params(params.userWalletId, stakingId)).bind()
    }

    override fun actionHandler(params: EarnProduct.Params, navigator: EarnNavigator): EarnActionHandler =
        EarnActionHandler { navigator.navigate(stakingRoute(params.userWalletId, params.currency)) }

    /** Availability does not depend on balances, so it is resubscribed only when the set of pool currencies changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun availabilities(
        userWalletId: UserWalletId,
        poolStatuses: Flow<List<CryptoCurrencyStatus>>,
    ): Flow<Map<CryptoCurrency.ID, StakingAvailability>> = poolStatuses
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

    private fun CryptoCurrencyStatus.isPoolCurrency(): Boolean =
        currency is CryptoCurrency.Coin && StakingIntegrationID.create(currency.id) == StakingIntegrationID.P2PEthPool
}