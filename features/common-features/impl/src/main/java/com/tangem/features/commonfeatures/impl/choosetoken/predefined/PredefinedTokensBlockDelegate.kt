package com.tangem.features.commonfeatures.impl.choosetoken.predefined

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.PredefinedTokenToAdd
import com.tangem.features.commonfeatures.impl.choosetoken.AddToPortfolioRoute
import com.tangem.features.commonfeatures.impl.choosetoken.predefined.state.PredefinedTokenItemUM
import com.tangem.features.commonfeatures.impl.choosetoken.predefined.state.PredefinedTokensUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Suppress("LongParameterList")
internal class PredefinedTokensBlockDelegate @AssistedInject constructor(
    @Assisted private val predefinedTokens: StateFlow<List<PredefinedTokenToAdd>>,
    @Assisted private val searchQueryState: StateFlow<SearchQuery>,
    @Assisted private val addToPortfolioManager: AddToPortfolioManager,
    @Assisted private val addToPortfolioSlot: SlotNavigation<AddToPortfolioRoute>,
    @Assisted private val modelScope: CoroutineScope,
    @Assisted private val tokenFilter: MutableStateFlow<(AccountStatus, CryptoCurrencyStatus) -> Boolean>,
    @Assisted private val portfolioTokenKeys: Flow<Set<Pair<String, String>>>,
) {

    init {
        predefinedTokens
            .onEach { tokens -> tokenFilter.value = buildTokenFilter(tokens) }
            .launchIn(modelScope)
    }

    val stateFlow: Flow<PredefinedTokensUM?> = combine(
        predefinedTokens,
        searchQueryState,
        portfolioTokenKeys,
    ) { tokens, query, portfolioKeys ->
        val filtered = tokens.filter { token ->
            token.hasValidNetwork() &&
                token.matchesQuery(query.value) &&
                !portfolioKeys.contains(token.toKey())
        }
        if (filtered.isEmpty()) {
            null
        } else {
            PredefinedTokensUM(items = filtered.map { it.toItemUM() }.toImmutableList())
        }
    }

    private fun buildTokenFilter(
        tokens: List<PredefinedTokenToAdd>,
    ): (AccountStatus, CryptoCurrencyStatus) -> Boolean {
        val tokenKeys = tokens
            .filter { it.hasValidNetwork() }
            .mapTo(hashSetOf()) { it.token.id.value to it.network.networkId }
        if (tokenKeys.isEmpty()) return { _, _ -> true }
        return filter@{ _, currencyStatus ->
            val rawId = currencyStatus.currency.id.rawCurrencyId?.value ?: return@filter false
            tokenKeys.contains(rawId to currencyStatus.currency.network.rawId)
        }
    }

    /** Identity of a predefined token as `(rawCurrencyId, networkId)` — matches the portfolio token keys. */
    private fun PredefinedTokenToAdd.toKey(): Pair<String, String> = token.id.value to network.networkId

    private fun PredefinedTokenToAdd.hasValidNetwork(): Boolean =
        network.networkId.isNotBlank() && network.decimalCount != null

    private fun PredefinedTokenToAdd.matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        return token.symbol.contains(query, ignoreCase = true) ||
            token.name.contains(query, ignoreCase = true)
    }

    private fun PredefinedTokenToAdd.toItemUM(): PredefinedTokenItemUM {
        val item = this
        val networkId = network.networkId
        val networkName = Blockchain.fromNetworkId(networkId)?.fullName?.takeIf { it.isNotBlank() } ?: networkId
        return PredefinedTokenItemUM(
            id = "${token.id.value}_$networkId",
            symbol = token.symbol,
            networkName = TextReference.Str(networkName),
            networkId = networkId,
            iconUrl = iconUrl,
            onAddClick = { onAddClick(item) },
        )
    }

    private fun onAddClick(item: PredefinedTokenToAdd) {
        addToPortfolioManager.setTokenParams(item.token)
        addToPortfolioManager.setTokenNetworks(listOf(item.network))
        addToPortfolioSlot.activate(AddToPortfolioRoute)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            predefinedTokens: StateFlow<List<PredefinedTokenToAdd>>,
            searchQueryState: StateFlow<SearchQuery>,
            addToPortfolioManager: AddToPortfolioManager,
            addToPortfolioSlot: SlotNavigation<AddToPortfolioRoute>,
            modelScope: CoroutineScope,
            tokenFilter: MutableStateFlow<(AccountStatus, CryptoCurrencyStatus) -> Boolean>,
            portfolioTokenKeys: Flow<Set<Pair<String, String>>>,
        ): PredefinedTokensBlockDelegate
    }
}