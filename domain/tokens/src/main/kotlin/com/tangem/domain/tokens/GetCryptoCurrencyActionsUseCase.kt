package com.tangem.domain.tokens

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.StatusSource
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Use case to determine which TokenActions are available for a [CryptoCurrency]
 *
 * @property rampManager Ramp manager to check ramp availability
 */
@Suppress("LongParameterList")
class GetCryptoCurrencyActionsUseCase(
    private val rampManager: RampStateManager,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val stakingRepository: StakingRepository,
    private val promoRepository: PromoRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val swapFeatureToggles: SwapFeatureToggles,
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
            return getActionsForOutdatedData(userWallet, cryptoCurrencyStatus, requirements)
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
        if (userWallet.isMultiCurrency) {
            val isExchangeable = withTimeoutOrNull(REQUEST_EXCHANGE_DATA_TIMEOUT) {
                rampManager.availableForSwap(userWallet.walletId, cryptoCurrency)
            } ?: false
            if (isExchangeable && cryptoCurrencyStatus.value !is CryptoCurrencyStatus.NoQuote) {
                val swapStoriesEnabled = swapFeatureToggles.isPromoStoriesEnabled
                activeList.add(
                    TokenActionsState.ActionState.Swap(
                        unavailabilityReason = ScenarioUnavailabilityReason.None,
                        showBadge = shouldShowSwapStories && swapStoriesEnabled,
                    ),
                )
            } else {
                disabledList.add(
                    TokenActionsState.ActionState.Swap(
                        unavailabilityReason = ScenarioUnavailabilityReason.NotExchangeable(cryptoCurrency.name),
                        showBadge = false,
                    ),
                )
            }
        }

        // buy
        if (rampManager.availableForBuy(userWallet.scanResponse, userWallet.walletId, cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None))
        } else {
            disabledList.add(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrency.symbol)),
            )
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

    private suspend fun getActionsForUnreachableCurrency(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        requirements: AssetRequirementsCondition?,
    ): List<TokenActionsState.ActionState> {
        val actionsList = mutableListOf<TokenActionsState.ActionState>()

        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }
        if (rampManager.availableForBuy(userWallet.scanResponse, userWallet.walletId, cryptoCurrencyStatus.currency)) {
            actionsList.add(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None))
        } else {
            actionsList.add(
                TokenActionsState.ActionState.Buy(
                    ScenarioUnavailabilityReason.BuyUnavailable(
                        cryptoCurrencyName = cryptoCurrencyStatus.currency.name,
                    ),
                ),
            )
        }
        actionsList.add(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.Unreachable))
        actionsList.add(
            TokenActionsState.ActionState.Swap(
                unavailabilityReason = ScenarioUnavailabilityReason.Unreachable,
                showBadge = false,
            ),
        )
        actionsList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.Unreachable))
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            val scenario = getReceiveScenario(requirements)
            actionsList.add(TokenActionsState.ActionState.Receive(scenario))
        }
        actionsList.add(TokenActionsState.ActionState.Stake(ScenarioUnavailabilityReason.Unreachable, null))
        actionsList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return actionsList
    }

    private suspend fun getActionsForOutdatedData(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        requirements: AssetRequirementsCondition?,
    ): List<TokenActionsState.ActionState> {
        val activeList = mutableListOf<TokenActionsState.ActionState>()

        // copy address
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            activeList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }

        // receive
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            val scenario = getReceiveScenario(requirements)
            activeList.add(TokenActionsState.ActionState.Receive(scenario))
        }

        // swap
        val isSwapAvailable = with(cryptoCurrencyStatus.value.sources) {
            quoteSource.isActual() && networkSource.isActual()
        }

        activeList.add(
            TokenActionsState.ActionState.Swap(
                unavailabilityReason = if (isSwapAvailable) {
                    ScenarioUnavailabilityReason.None
                } else {
                    ScenarioUnavailabilityReason.UsedOutdatedData
                },
                showBadge = false,
            ),
        )

        // buy
        if (rampManager.availableForBuy(userWallet.scanResponse, userWallet.walletId, cryptoCurrencyStatus.currency)) {
            activeList.add(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None))
        } else {
            activeList.add(
                TokenActionsState.ActionState.Buy(
                    ScenarioUnavailabilityReason.BuyUnavailable(
                        cryptoCurrencyName = cryptoCurrencyStatus.currency.name,
                    ),
                ),
            )
        }

        // staking
        activeList.add(TokenActionsState.ActionState.Stake(ScenarioUnavailabilityReason.UsedOutdatedData, null))

        // send
        val isSendAvailable = cryptoCurrencyStatus.value.sources.networkSource.isActual()

        activeList.add(
            TokenActionsState.ActionState.Send(
                unavailabilityReason = if (isSendAvailable) {
                    ScenarioUnavailabilityReason.None
                } else {
                    ScenarioUnavailabilityReason.UsedOutdatedData
                },
            ),
        )

        // region sell
        activeList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.UsedOutdatedData))
        // endregion

        // hide
        activeList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return activeList
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

    private fun isAddressAvailable(networkAddress: NetworkAddress?): Boolean {
        return networkAddress != null && networkAddress.defaultAddress.value.isNotEmpty()
    }

    private companion object {
        const val REQUEST_EXCHANGE_DATA_TIMEOUT = 1000L
    }
}