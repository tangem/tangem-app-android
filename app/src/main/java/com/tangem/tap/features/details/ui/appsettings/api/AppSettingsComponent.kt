package com.tangem.tap.features.details.ui.appsettings.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface AppSettingsComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, AppSettingsComponent>
}