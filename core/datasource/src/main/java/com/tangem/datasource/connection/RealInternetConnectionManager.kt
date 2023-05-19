package com.tangem.datasource.connection

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow

internal class RealInternetConnectionManager : NetworkConnectionManager {

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO + FeatureCoroutineExceptionHandler.create("RealInternetConnectionManager"),
        )

    override val isOnlineFlow: StateFlow<Boolean> = ReactiveNetwork
        .observeInternetConnectivity()
        .toFlowable(BackpressureStrategy.LATEST)
        .asFlow()
        .stateIn(scope, SharingStarted.Eagerly, initialValue = false)

    override val isOnline: Boolean
        get() = isOnlineFlow.value
}