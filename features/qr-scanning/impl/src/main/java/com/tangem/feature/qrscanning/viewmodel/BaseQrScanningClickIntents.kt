package com.tangem.feature.qrscanning.viewmodel

import androidx.activity.result.ActivityResultLauncher
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.feature.qrscanning.navigation.QrScanningInnerRouter
import kotlinx.coroutines.CoroutineScope
import kotlin.properties.Delegates

internal open class BaseQrScanningClickIntents {

    protected val router: QrScanningInnerRouter get() = _router
    protected val viewModelScope: CoroutineScope get() = _viewModelScope
    protected val source: SourceType get() = _source
    protected val galleryLauncher: ActivityResultLauncher<String> get() = _galleryLauncher

    private var _router: QrScanningInnerRouter by Delegates.notNull()
    private var _viewModelScope: CoroutineScope by Delegates.notNull()
    private var _source: SourceType by Delegates.notNull()

    private var _galleryLauncher: ActivityResultLauncher<String> by Delegates.notNull()

    open fun initialize(
        router: QrScanningInnerRouter,
        source: SourceType,
        galleryLauncher: ActivityResultLauncher<String>,
        coroutineScope: CoroutineScope,
    ) {
        _router = router
        _viewModelScope = coroutineScope
        _source = source
        _galleryLauncher = galleryLauncher
    }
}