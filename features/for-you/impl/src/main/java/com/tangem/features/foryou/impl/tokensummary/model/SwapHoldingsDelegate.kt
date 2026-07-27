package com.tangem.features.foryou.impl.tokensummary.model

import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Resolves what the summary token's bottom button can offer: topping up, swapping, or nothing.
 *
 * Holdings of the token are collected from every wallet and kept up to date, so the state settles on its own as
 * balances arrive. Swap availability is delegated to [GetCryptoCurrencyActionsUseCase] — the same source the Swap
 * button uses everywhere else — and is only consulted once at least one holding has funds, since there is nothing to
 * swap from otherwise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapHoldingsDelegate @AssistedInject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    dispatchers: CoroutineDispatcherProvider,
    @Assisted modelScope: CoroutineScope,
    @Assisted private val token: TokenSummaryComponent.Token,
) {

    private val rawCurrencyId: CryptoCurrency.RawID? = token.rawCurrencyId
    private val network: Network? = token.network
    private val summaryTokenId: String? = rawCurrencyId?.value?.let(::getTokenIdIfL2Network)

    val state: StateFlow<SwapHoldingsState> = buildStateFlow()
        .flowOn(dispatchers.default)
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = SwapHoldingsState.Loading)

    private fun buildStateFlow(): Flow<SwapHoldingsState> {
        if (rawCurrencyId == null) return flowOf(SwapHoldingsState.Unavailable)

        return multiAccountStatusListSupplier()
            .map(::collectHoldings)
            .distinctUntilChanged()
            .flatMapLatest(::resolveState)
    }

    private suspend fun collectHoldings(accountLists: List<AccountStatusList>): List<TokenSelectorEntry> {
        val walletsById = userWalletsListRepository.userWalletsSync().associateBy(UserWallet::walletId)

        return accountLists.flatMap { accountList ->
            val wallet = walletsById[accountList.userWalletId] ?: return@flatMap emptyList()

            accountList.accountStatuses.filterCryptoPortfolio().flatMap { accountStatus ->
                accountStatus.tokenList.flattenCurrencies()
                    .filter(::matchesSummaryToken)
                    .map { status ->
                        TokenSelectorEntry(wallet = wallet, account = accountStatus, currencyStatus = status)
                    }
            }
        }
    }

    private fun resolveState(holdings: List<TokenSelectorEntry>): Flow<SwapHoldingsState> {
        return when {
            holdings.isEmpty() -> flowOf(SwapHoldingsState.Unavailable)
            holdings.all { it.currencyStatus.hasZeroBalance() } -> flowOf(SwapHoldingsState.ZeroBalance)
            else -> combine(holdings.map(::resolveSwappableHolding), ::resolveAvailability)
        }
    }

    private fun resolveSwappableHolding(holding: TokenSelectorEntry): Flow<TokenSelectorEntry?> =
        getCryptoCurrencyActionsUseCase(userWallet = holding.wallet, cryptoCurrencyStatus = holding.currencyStatus)
            .map { actions ->
                val isSwapAvailable = actions.states
                    .filterIsInstance<TokenActionsState.ActionState.Swap>()
                    .any { it.unavailabilityReason == ScenarioUnavailabilityReason.None }

                holding.takeIf { isSwapAvailable }
            }
            .distinctUntilChanged()

    private fun resolveAvailability(holdings: Array<TokenSelectorEntry?>): SwapHoldingsState {
        val swappable = holdings.filterNotNull()

        return if (swappable.isEmpty()) SwapHoldingsState.Unavailable else SwapHoldingsState.Available(swappable)
    }

    private fun matchesSummaryToken(status: CryptoCurrencyStatus): Boolean {
        val holdingRawId = status.currency.id.rawCurrencyId ?: return false
        val isMatchesRawId = getTokenIdIfL2Network(holdingRawId.value) == summaryTokenId
        val isMatchesNetwork = network == null || status.currency.network.rawId == network.rawId

        return isMatchesRawId && isMatchesNetwork
    }

    private fun CryptoCurrencyStatus.hasZeroBalance(): Boolean = value.amount.orZero().signum() == 0

    @AssistedFactory
    interface Factory {
        fun create(modelScope: CoroutineScope, token: TokenSummaryComponent.Token): SwapHoldingsDelegate
    }
}