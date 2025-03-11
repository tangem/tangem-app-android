package com.tangem.feature.qrscanning.viewmodel

import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.feature.qrscanning.navigation.QrScanningInnerRouter
import kotlinx.coroutines.CoroutineScope
import kotlin.properties.Delegates

internal open class BaseQrScanningClickIntents {

    protected val router: QrScanningInnerRouter get() = _router
    protected val viewModelScope: CoroutineScope get() = _viewModelScope
    protected val source: SourceType get() = _source

    private var _router: QrScanningInnerRouter by Delegates.notNull()
    private var _viewModelScope: CoroutineScope by Delegates.notNull()
    private var _source: SourceType by Delegates.notNull()

    open fun initialize(router: QrScanningInnerRouter, source: SourceType, coroutineScope: CoroutineScope) {
        _router = router
        _viewModelScope = coroutineScope
        _source = source
    }
}