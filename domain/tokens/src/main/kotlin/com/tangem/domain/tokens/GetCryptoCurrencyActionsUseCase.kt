package com.tangem.domain.tokens

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.StoryContent
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.actions.CommonActionsFactory
import com.tangem.domain.tokens.actions.MissedDerivationsActionsFactory
import com.tangem.domain.tokens.actions.OutdatedDataActionsFactory
import com.tangem.domain.tokens.actions.UnreachableActionsFactory
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Use case for retrieving actions available for a specific cryptocurrency in a user's wallet.
 *
 * @param rampManager          the manager for handling ramp state operations
 * @param walletManagersFacade the facade for managing wallet operations
 * @property stakingRepository the repository for staking-related data
 * @property promoRepository   the repository for promotional content
 * @property dispatchers       the coroutine dispatcher provider for managing concurrency
 */
class GetCryptoCurrencyActionsUseCase(
    rampManager: RampStateManager,
    walletManagersFacade: WalletManagersFacade,
    private val stakingRepository: StakingRepository,
    private val promoRepository: PromoRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val unreachableActionsFactory = UnreachableActionsFactory(
        walletManagersFacade = walletManagersFacade,
        rampStateManager = rampManager,
    )

    private val outdatedDataActionsFactory = OutdatedDataActionsFactory(
        walletManagersFacade = walletManagersFacade,
        rampStateManager = rampManager,
    )

    private val commonActionsFactory = CommonActionsFactory(
        walletManagersFacade = walletManagersFacade,
        rampStateManager = rampManager,
    )

    operator fun invoke(userWallet: UserWallet, cryptoCurrencyStatus: CryptoCurrencyStatus): Flow<TokenActionsState> {
        return when (userWallet) {
            is UserWallet.Cold -> coldFlow(userWallet, cryptoCurrencyStatus)
            is UserWallet.Hot -> TODO("[REDACTED_TASK_KEY]")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun coldFlow(
        userWallet: UserWallet.Cold,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Flow<TokenActionsState> {
        return when {
            cryptoCurrencyStatus.value is CryptoCurrencyStatus.MissedDerivation -> {
                flowOf(value = MissedDerivationsActionsFactory.create())
            }
            cryptoCurrencyStatus.value is CryptoCurrencyStatus.Unreachable -> {
                flow {
                    val actions = unreachableActionsFactory.create(
                        userWallet = userWallet,
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                    )

                    emit(actions)
                }
            }
            cryptoCurrencyStatus.value.sources.total != StatusSource.ACTUAL -> {
                getStakingAvailabilityFlow(
                    userWalletId = userWallet.walletId,
                    currency = cryptoCurrencyStatus.currency,
                )
                    .mapLatest {
                        outdatedDataActionsFactory.create(
                            userWallet = userWallet,
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                            stakingAvailability = it,
                        )
                    }
            }
            else -> {
                combine(
                    flow = getStakingAvailabilityFlow(
                        userWalletId = userWallet.walletId,
                        currency = cryptoCurrencyStatus.currency,
                    ),
                    flow2 = getSwapStoryContent(),
                ) { stakingAvailability, swapStoryContent ->
                    commonActionsFactory.create(
                        userWallet = userWallet,
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        stakingAvailability = stakingAvailability,
                        shouldShowSwapStories = swapStoryContent != null,
                    )
                }
            }
        }
            .map {
                TokenActionsState(
                    walletId = userWallet.walletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    states = it.toList(),
                )
            }
            .flowOn(dispatchers.default)
    }

    private fun getStakingAvailabilityFlow(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Flow<StakingAvailability> {
        return stakingRepository.getStakingAvailability(userWalletId = userWalletId, cryptoCurrency = currency)
            .onStart { emit(StakingAvailability.Unavailable) }
            .conflate()
            .distinctUntilChanged()
    }

    private fun getSwapStoryContent(): Flow<StoryContent?> {
        return promoRepository.getStoryById(StoryContentIds.STORY_FIRST_TIME_SWAP.id)
            .conflate()
            .distinctUntilChanged()
    }
}