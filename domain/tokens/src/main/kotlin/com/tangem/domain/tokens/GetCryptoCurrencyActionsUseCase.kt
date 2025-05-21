package com.tangem.domain.tokens

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Use case to determine which TokenActions are available for a [CryptoCurrency]
 *
 * @property rampManager Ramp manager to check ramp availability
 */
@Suppress("LongParameterList", "LargeClass")
class GetCryptoCurrencyActionsUseCase(
    private val rampManager: RampStateManager,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val stakingRepository: StakingRepository,
    private val promoRepository: PromoRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Flow<TokenActionsState> {
        val networkId = cryptoCurrencyStatus.currency.network.id
        val requirements = withTimeoutOrNull(REQUEST_EXCHANGE_DATA_TIMEOUT) {
            walletManagersFacade.getAssetRequirements(userWallet.walletId, cryptoCurrencyStatus.currency)
        }
        return flow {
            val networkFlow = if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                currencyStatusOperations.getNetworkCoinForSingleWalletWithTokenFlow(userWallet.walletId, networkId)
            } else if (!userWallet.isMultiCurrency) {
                currencyStatusOperations.getPrimaryCurrencyStatusFlow(userWallet.walletId, includeQuotes = false)
            } else {
                currencyStatusOperations.getNetworkCoinFlow(
                    userWalletId = userWallet.walletId,
                    networkId = networkId,
                    derivationPath = cryptoCurrencyStatus.currency.network.derivationPath,
                    includeQuotes = false,
                )
            }
            val flow = combine(
                flow = networkFlow,
                flow2 = promoRepository.getStoryById(StoryContentIds.STORY_FIRST_TIME_SWAP.id).conflate(),
                flow3 = stakingRepository.getStakingAvailability(
                    userWalletId = userWallet.walletId,
                    cryptoCurrency = cryptoCurrencyStatus.currency,
                ).onStart { emit(StakingAvailability.Unavailable) },
            ) { maybeCoinStatus, maybeSwapStories, stakingAvailability ->
                createTokenActionsState(
                    userWallet = userWallet,
                    coinStatus = maybeCoinStatus.getOrNull(),
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    requirements = requirements,
                    shouldShowSwapStories = maybeSwapStories != null,
                    isStakingAvailable = stakingAvailability is StakingAvailability.Available,
                )
            }

            emitAll(flow)
        }.flowOn(dispatchers.io)
    }

    private suspend fun createTokenActionsState(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        requirements: AssetRequirementsCondition?,
        shouldShowSwapStories: Boolean,
        isStakingAvailable: Boolean,
    ): TokenActionsState {
        return TokenActionsState(
            walletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            states = createListOfActions(
                userWallet = userWallet,
                coinStatus = coinStatus,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                requirements = requirements,
                shouldShowSwapStories = shouldShowSwapStories,
                isStakingAvailable = isStakingAvailable,
            ),
        )
    }

    /**
     * Creates list of action for expected order
     * Actions priority: [Receive Send Swap Buy Sell]
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private suspend fun createListOfActions(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        requirements: AssetRequirementsCondition?,
        shouldShowSwapStories: Boolean,
        isStakingAvailable: Boolean,
    ): List<TokenActionsState.ActionState> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.MissedDerivation) {
            return listOf(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))
        }
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Unreachable) {
            return getActionsForUnreachableCurrency(userWallet, cryptoCurrencyStatus, requirements)
        }

        if (cryptoCurrencyStatus.value.sources.total != StatusSource.ACTUAL) {
            return getActionsForOutdatedData(userWallet, cryptoCurrencyStatus, requirements, isStakingAvailable)
        }

        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()

        // markets
        // not a custom token
        if (cryptoCurrencyStatus.currency.id.rawCurrencyId != null) {
            activeList.add(TokenActionsState.ActionState.Analytics(ScenarioUnavailabilityReason.None))
        }

        // copy address
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            activeList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }

        // receive
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            val scenario = getReceiveScenario(requirements)
            activeList.add(TokenActionsState.ActionState.Receive(scenario))
        }

        // staking
        addStakingActions(cryptoCurrency, isStakingAvailable, activeList, disabledList)

        // send
        val sendUnavailabilityReason = getSendUnavailabilityReason(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            coinStatus = coinStatus,
        )
        if (sendUnavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(TokenActionsState.ActionState.Send(sendUnavailabilityReason))
        } else {
            disabledList.add(TokenActionsState.ActionState.Send(sendUnavailabilityReason))
        }

        // swap
        val swapActionState = getSwapUnavailabilityReason(userWallet, cryptoCurrencyStatus, shouldShowSwapStories)
        if (swapActionState.unavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(swapActionState)
        } else {
            disabledList.add(swapActionState)
        }

        // buy
        val onrampActionState = getOnrampUnavailabilityReason(userWallet, cryptoCurrencyStatus)
        if (onrampActionState.unavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(onrampActionState)
        } else {
            disabledList.add(onrampActionState)
        }

        // region sell
        rampManager.availableForSell(
            userWalletId = userWallet.walletId,
            status = cryptoCurrencyStatus,
        )
            .onRight {
                activeList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None))
            }
            .onLeft { reason ->
                disabledList.add(TokenActionsState.ActionState.Sell(reason))
            }
        // endregion

        // hide
        activeList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return activeList + disabledList
    }

    private suspend fun addStakingActions(
        cryptoCurrency: CryptoCurrency,
        isStakingAvailable: Boolean,
        activeList: MutableList<TokenActionsState.ActionState>,
        disabledList: MutableList<TokenActionsState.ActionState>,
    ) {
        if (isStakingAvailable) {
            val yield = kotlin.runCatching {
                stakingRepository.getYield(
                    cryptoCurrencyId = cryptoCurrency.id,
                    symbol = cryptoCurrency.symbol,
                )
            }.getOrNull()
            activeList.add(
                TokenActionsState.ActionState.Stake(
                    unavailabilityReason = ScenarioUnavailabilityReason.None,
                    yield = yield,
                ),
            )
        } else {
            disabledList.add(
                TokenActionsState.ActionState.Stake(
                    unavailabilityReason = ScenarioUnavailabilityReason.StakingUnavailable(cryptoCurrency.name),
                    yield = null,
                ),
            )
        }
    }

    private suspend fun getActionsForUnreachableCurrency(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        requirements: AssetRequirementsCondition?,
    ): List<TokenActionsState.ActionState> {
        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()

        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            activeList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }

        // buy (is not depend on cache)
        val onrampActionState = getOnrampUnavailabilityReason(userWallet, cryptoCurrencyStatus)
        if (onrampActionState.unavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(onrampActionState)
        } else {
            disabledList.add(onrampActionState)
        }

        disabledList.add(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.Unreachable))
        disabledList.add(
            TokenActionsState.ActionState.Swap(
                unavailabilityReason = ScenarioUnavailabilityReason.Unreachable,
                showBadge = false,
            ),
        )
        disabledList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.Unreachable))
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            val scenario = getReceiveScenario(requirements)
            activeList.add(TokenActionsState.ActionState.Receive(scenario))
        }
        disabledList.add(TokenActionsState.ActionState.Stake(ScenarioUnavailabilityReason.Unreachable, null))
        activeList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return activeList + disabledList
    }

    @Suppress("LongMethod")
    private suspend fun getActionsForOutdatedData(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        requirements: AssetRequirementsCondition?,
        isStakingAvailable: Boolean,
    ): List<TokenActionsState.ActionState> {
        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()
        val cryptoCurrency = cryptoCurrencyStatus.currency

        // copy address
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            activeList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }

        // receive
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            val scenario = getReceiveScenario(requirements)
            val action = TokenActionsState.ActionState.Receive(scenario)
            if (scenario == ScenarioUnavailabilityReason.None) {
                activeList.add(action)
            } else {
                disabledList.add(action)
            }
        }

        // swap
        val sources = cryptoCurrencyStatus.value.sources
        val isSwapAvailable = with(sources) {
            quoteSource.isActual() && networkSource.isActual()
        }

        val swapAction = TokenActionsState.ActionState.Swap(
            unavailabilityReason = if (isSwapAvailable) {
                ScenarioUnavailabilityReason.None
            } else if (sources.networkSource == StatusSource.ONLY_CACHE) {
                ScenarioUnavailabilityReason.UsedOutdatedData
            } else {
                // CACHE source always when loading
                ScenarioUnavailabilityReason.DataLoading
            },
            showBadge = false,
        )
        if (swapAction.unavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(swapAction)
        } else {
            disabledList.add(swapAction)
        }

        // buy (is not depend on cache)
        val onrampActionState = getOnrampUnavailabilityReason(userWallet, cryptoCurrencyStatus)
        if (onrampActionState.unavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(onrampActionState)
        } else {
            disabledList.add(onrampActionState)
        }

        // staking
        if (cryptoCurrencyStatus.value.sources.networkSource.isActual()) {
            addStakingActions(cryptoCurrency, isStakingAvailable, activeList, disabledList)
        } else {
            disabledList.add(TokenActionsState.ActionState.Stake(ScenarioUnavailabilityReason.UsedOutdatedData, null))
        }

        // send
        val isSendAvailable = cryptoCurrencyStatus.value.sources.networkSource.isActual()

        val sendAction = TokenActionsState.ActionState.Send(
            unavailabilityReason = if (isSendAvailable) {
                ScenarioUnavailabilityReason.None
            } else {
                ScenarioUnavailabilityReason.UsedOutdatedData
            },
        )
        if (sendAction.unavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(sendAction)
        } else {
            disabledList.add(sendAction)
        }

        // region sell
        if (isSendAvailable) {
            activeList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None))
        } else {
            disabledList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.UsedOutdatedData))
        }
        // endregion

        // hide
        activeList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return activeList + disabledList
    }

    private fun getReceiveScenario(requirements: AssetRequirementsCondition?): ScenarioUnavailabilityReason {
        return if (requirements is AssetRequirementsCondition.PaidTransaction ||
            requirements is AssetRequirementsCondition.PaidTransactionWithFee
        ) {
            ScenarioUnavailabilityReason.UnassociatedAsset
        } else {
            ScenarioUnavailabilityReason.None
        }
    }

    private fun getSendUnavailabilityReason(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): ScenarioUnavailabilityReason {
        return when {
            cryptoCurrencyStatus.value.amount.isNullOrZero() -> {
                ScenarioUnavailabilityReason.EmptyBalance(ScenarioUnavailabilityReason.WithdrawalScenario.SEND)
            }
            currenciesRepository.isSendBlockedByPendingTransactions(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                coinStatus = coinStatus,
            ) -> {
                ScenarioUnavailabilityReason.PendingTransaction(
                    withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SEND,
                    networkName = coinStatus?.currency?.network?.name.orEmpty(),
                )
            }
            else -> {
                ScenarioUnavailabilityReason.None
            }
        }
    }

    private suspend fun getSwapUnavailabilityReason(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        shouldShowSwapStories: Boolean,
    ): TokenActionsState.ActionState {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        return if (userWallet.isMultiCurrency) {
            if (cryptoCurrency.isCustom) {
                return TokenActionsState.ActionState.Swap(
                    unavailabilityReason = ScenarioUnavailabilityReason.CustomToken(cryptoCurrency.name),
                    showBadge = false,
                )
            }
            if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.NoQuote) {
                return TokenActionsState.ActionState.Swap(
                    unavailabilityReason = ScenarioUnavailabilityReason.TokenNoQuotes(cryptoCurrency.name),
                    showBadge = false,
                )
            }
            val reason = rampManager.availableForSwap(userWallet.walletId, cryptoCurrency)
            val isShowBadge = reason == ScenarioUnavailabilityReason.None && shouldShowSwapStories
            TokenActionsState.ActionState.Swap(
                unavailabilityReason = reason,
                showBadge = isShowBadge,
            )
        } else {
            TokenActionsState.ActionState.Swap(
                unavailabilityReason = ScenarioUnavailabilityReason.SingleWallet,
                showBadge = false,
            )
        }
    }

    private suspend fun getOnrampUnavailabilityReason(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): TokenActionsState.ActionState {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val reason = rampManager.availableForBuy(userWallet.scanResponse, userWallet.walletId, cryptoCurrency)
        return TokenActionsState.ActionState.Buy(unavailabilityReason = reason)
    }

    private fun isAddressAvailable(networkAddress: NetworkAddress?): Boolean {
        return networkAddress != null && networkAddress.defaultAddress.value.isNotEmpty()
    }

    private companion object {
        const val REQUEST_EXCHANGE_DATA_TIMEOUT = 1000L
    }
}