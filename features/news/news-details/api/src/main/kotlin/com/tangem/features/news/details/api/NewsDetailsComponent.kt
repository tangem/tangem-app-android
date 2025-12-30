package com.tangem.features.news.details.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface NewsDetailsComponent : ComposableContentComponent {

    data class Params(val selectedArticleId: Int = 0)

    interface Factory : ComponentFactory<Params, NewsDetailsComponent>
}