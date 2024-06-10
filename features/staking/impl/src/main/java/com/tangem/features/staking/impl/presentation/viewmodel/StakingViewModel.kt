package com.tangem.features.staking.impl.presentation.viewmodel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.staking.api.navigation.StakingRouter
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.StakingStateRouter
import com.tangem.features.staking.impl.presentation.state.transformers.HideBalanceStateTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
internal class StakingViewModel @Inject constructor(
    private val stateController: StakingStateController,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getCryptoCurrencyStatusSyncUseCase: GetCryptoCurrencyStatusSyncUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, StakingClickIntents {

    val uiState: StateFlow<StakingUiState> = stateController.uiState
    val value: StakingUiState get() = uiState.value

    var stakingStateRouter: StakingStateRouter by Delegates.notNull()
        private set

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[StakingRouter.CRYPTO_CURRENCY_KEY]
        ?: error("This screen can't open without `CryptoCurrency`")

    private val userWalletId: UserWalletId = savedStateHandle.get<String>(StakingRouter.USER_WALLET_ID_KEY)
        ?.let { stringValue -> UserWalletId(stringValue) }
        ?: error("This screen can't open without `UserWalletId`")

    private var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()

    private var innerRouter: InnerStakingRouter by Delegates.notNull()

    init {
        subscribeOnBalanceHiding()
        subscribeOnCurrencyStatusUpdates()
    }

    fun setRouter(router: InnerStakingRouter, stakingStateRouter: StakingStateRouter) {
        innerRouter = router
        this.stakingStateRouter = stakingStateRouter
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        viewModelScope.launch {
            getCryptoCurrencyStatusSyncUseCase(userWalletId, cryptoCurrency.id).fold(
                ifRight = {
                    cryptoCurrencyStatus = it
                },
                ifLeft = {
                    // TODO staking error
                },
            )
        }
    }

    private fun subscribeOnBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                stateController.update(transformer = HideBalanceStateTransformer(it.isBalanceHidden))
            }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
    }
}