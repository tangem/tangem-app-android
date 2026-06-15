package com.tangem.features.survey

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface SurveyComponent : ComposableContentComponent {

    data class Params(val token: String, val displayId: String?)

    interface Factory : ComponentFactory<Params, SurveyComponent>
}