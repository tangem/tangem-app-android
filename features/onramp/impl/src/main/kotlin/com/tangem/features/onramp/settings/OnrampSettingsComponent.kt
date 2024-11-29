package com.tangem.features.onramp.settings

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

internal interface OnrampSettingsComponent : ComposableContentComponent {

    data class Params(val onBack: () -> Unit)

    interface Factory : ComponentFactory<Params, OnrampSettingsComponent>
}