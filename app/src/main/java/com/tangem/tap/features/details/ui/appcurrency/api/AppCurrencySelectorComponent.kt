package com.tangem.tap.features.details.ui.appcurrency.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface AppCurrencySelectorComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, AppCurrencySelectorComponent>
}