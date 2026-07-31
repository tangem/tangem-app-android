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
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.tokensummary.model.converter.SwapHoldingConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Resolves what the summary token's bottom button can offer: adding the token, topping it up, or swapping it.
 *
 * Holdings of the token are collected from every wallet and kept up to date, so the state settles on its own as
 * balances arrive. What a swap from a holding would run into is [SwapHoldingConverter]'s job, and it is only asked
 * once at least one holding has funds — there is nothing to swap from otherwise. An unavailable holding is kept with
 * its reason rather than dropped, so picking it can explain itself.
 */
internal class SwapHoldingsDelegate @AssistedInject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    dispatchers: CoroutineDispatcherProvider,
    @Assisted modelScope: CoroutineScope,
    @Assisted private val token: TokenSummaryComponent.Token,
) {

    private val rawCurrencyId: CryptoCurrency.RawID? = token.rawCurrencyId
    private val network: Network? = token.network
    private val summaryTokenId: String? = rawCurrencyId?.value?.let(::getTokenIdIfL2Network)

    private val holdingConverter = SwapHoldingConverter(getCryptoCurrencyActionsUseCase)

    val state: StateFlow<SwapHoldingsState> = buildStateFlow()
        .flowOn(dispatchers.default)
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = SwapHoldingsState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildStateFlow(): Flow<SwapHoldingsState> {
        if (rawCurrencyId == null) return flowOf(SwapHoldingsState.Unavailable)

        return multiAccountStatusListSupplier()
            .map(::collectHoldings)
            .distinctUntilChanged()
            .flatMapLatest(::resolveState)
    }

    private suspend fun collectHoldings(accountLists: List<AccountStatusList>): List<TokenSelectorEntry> {
        val walletsById = userWalletsListRepository.userWalletsSync()
            .filterNot(UserWallet::isLocked)
            .associateBy(UserWallet::walletId)

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
            holdings.isEmpty() -> flowOf(SwapHoldingsState.NotHeld)
            holdings.all { it.currencyStatus.hasZeroBalance() } -> flowOf(SwapHoldingsState.ZeroBalance)
            else -> combine(
                holdings.map(holdingConverter::convert),
            ) { SwapHoldingsState.Resolved(holdings = it.toList()) }
        }
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