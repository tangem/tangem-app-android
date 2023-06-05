package com.tangem.datasource.connection

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.InternetObservingSettings
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import io.reactivex.BackpressureStrategy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow

private const val PING_INTERVAL = 5_000

internal class RealInternetConnectionManager : NetworkConnectionManager {

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO + FeatureCoroutineExceptionHandler.create("RealInternetConnectionManager"),
        )

    private val initialNetworkResult: Boolean by lazy {
        ReactiveNetwork
            .checkInternetConnectivity()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .blockingGet()
    }

    override val isOnlineFlow: StateFlow<Boolean> = ReactiveNetwork
        .observeInternetConnectivity(
            InternetObservingSettings.builder()
                .interval(PING_INTERVAL)
                .build(),
        )
        .toFlowable(BackpressureStrategy.LATEST)
        .asFlow()
        .stateIn(scope, SharingStarted.Lazily, initialValue = initialNetworkResult)

    override val isOnline: Boolean
        get() = isOnlineFlow.value
}