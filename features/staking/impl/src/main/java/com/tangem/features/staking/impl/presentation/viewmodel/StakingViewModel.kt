package com.tangem.features.staking.impl.presentation.viewmodel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.staking.api.navigation.StakingRouter
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.StakingStateFactory
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
internal class StakingViewModel @Inject constructor(
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, StakingClickIntents {

    val uiState: StateFlow<StakingUiState> get() = mutableUiState
    val value: StakingUiState get() = uiState.value

    var stateRouter: StateRouter by Delegates.notNull()
        private set

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[StakingRouter.CRYPTO_CURRENCY_KEY]
        ?: error("This screen can't open without `CryptoCurrency`")

    private var balanceHidingJobHolder = JobHolder()

    private val stateFactory = StakingStateFactory(
        clickIntents = this,
        cryptoCurrency = cryptoCurrency,
        currentStateProvider = Provider { uiState.value },
    )

    private val mutableUiState: MutableStateFlow<StakingUiState> = MutableStateFlow(
        value = stateFactory.getInitialState(),
    )

    private var innerRouter: InnerStakingRouter by Delegates.notNull()

    init {
        subscribeOnBalanceHiding()
    }

    override fun onCleared() {
        balanceHidingJobHolder.cancel()
        super.onCleared()
    }

    fun setRouter(router: InnerStakingRouter, stateRouter: StateRouter) {
        innerRouter = router
        this.stateRouter = stateRouter
    }

    private fun subscribeOnBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                mutableUiState.update { stateFactory.getOnHideBalanceState(isBalanceHidden = it.isBalanceHidden) }
            }
            .launchIn(viewModelScope)
            .saveIn(balanceHidingJobHolder)
    }
}
