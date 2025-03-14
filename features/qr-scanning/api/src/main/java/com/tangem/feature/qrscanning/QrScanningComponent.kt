package com.tangem.feature.qrscanning

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.qrscanning.models.SourceType

interface QrScanningComponent : ComposableContentComponent {

    data class Params(
        val source: SourceType,
        val networkName: String? = null,
    )

    interface Factory : ComponentFactory<Params, QrScanningComponent>
}