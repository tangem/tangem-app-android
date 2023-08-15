package com.tangem.feature.wallet.presentation.organizetokens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.common.Provider
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.ToggleTokenListGroupingUseCase
import com.tangem.domain.tokens.ToggleTokenListSortingUseCase
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.CryptoCurrenciesIdsResolver
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.router.WalletRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class OrganizeTokensViewModel @Inject constructor(
    private val getTokenListUseCase: GetTokenListUseCase,
    private val toggleTokenListGroupingUseCase: ToggleTokenListGroupingUseCase,
    private val toggleTokenListSortingUseCase: ToggleTokenListSortingUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), OrganizeTokensIntents {

    lateinit var router: InnerWalletRouter

    private val selectedAppCurrencyFlow = createSelectedAppCurrencyFlow()

    private val stateHolder = OrganizeTokensStateHolder(
        stateFlowScope = viewModelScope,
        intents = this,
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        onSubscription = {
            bootstrapTokenList()
        },
    )

    private val userWalletId: UserWalletId by lazy {
        val userWalletIdValue: String = checkNotNull(savedStateHandle[WalletRoute.userWalletIdKey])

        UserWalletId(userWalletIdValue)
    }

    private var tokenList: TokenList? = null

    val uiState: StateFlow<OrganizeTokensState> = stateHolder.stateFlow

    override fun onBackClick() {
        router.popBackStack()
    }

    override fun onSortClick() {
        viewModelScope.launch(Dispatchers.Default) {
            val list = tokenList ?: return@launch

            toggleTokenListSortingUseCase(list).fold(
                ifLeft = stateHolder::updateStateWithError,
                ifRight = {
                    stateHolder.updateStateWithTokenList(it)
                    tokenList = it
                },
            )
        }
    }

    override fun onGroupClick() {
        viewModelScope.launch(Dispatchers.Default) {
            val list = tokenList ?: return@launch

            toggleTokenListGroupingUseCase(list).fold(
                ifLeft = stateHolder::updateStateWithError,
                ifRight = {
                    stateHolder.updateStateWithTokenList(it)
                    tokenList = it
                },
            )
        }
    }

    override fun onApplyClick() {
        viewModelScope.launch(Dispatchers.Default) {
            stateHolder.updateStateToDisplayProgress()

            val listState = uiState.value.itemsState
            val resolver = CryptoCurrenciesIdsResolver()

            val result = applyTokenListSortingUseCase(
                userWalletId = userWalletId,
                sortedTokensIds = resolver.resolve(listState, tokenList),
                isGroupedByNetwork = listState is OrganizeTokensListState.GroupedByNetwork,
                isSortedByBalance = uiState.value.header.isSortedByBalance,
            )

            result.fold(
                ifLeft = stateHolder::updateStateWithError,
                ifRight = {
                    stateHolder.updateStateToHideProgress()
                    withContext(Dispatchers.Main) { router.popBackStack() }
                },
            )
        }
    }

    override fun onCancelClick() {
        router.popBackStack()
    }

    private fun bootstrapTokenList() {
        viewModelScope.launch(Dispatchers.Default) {
            val maybeTokenList = getTokenListUseCase(userWalletId)
                .first { it.getOrNull()?.totalFiatBalance is TokenList.FiatBalance.Loaded }

            maybeTokenList.fold(
                ifLeft = stateHolder::updateStateWithError,
                ifRight = {
                    stateHolder.updateStateWithTokenList(it)
                    tokenList = it
                },
            )
        }
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map { maybeAppCurrency ->
                maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppCurrency.Default,
            )
    }
}
