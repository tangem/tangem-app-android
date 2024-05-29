package com.tangem.features.staking.impl.presentation.viewmodel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.StateRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
internal class StakingViewModel @Inject constructor() :
    ViewModel(), DefaultLifecycleObserver, StakingClickIntents {

    private var innerRouter: InnerStakingRouter by Delegates.notNull()
    var stateRouter: StateRouter by Delegates.notNull()
        private set

    fun setRouter(router: InnerStakingRouter, stateRouter: StateRouter) {
        innerRouter = router
        this.stateRouter = stateRouter
    }
}
