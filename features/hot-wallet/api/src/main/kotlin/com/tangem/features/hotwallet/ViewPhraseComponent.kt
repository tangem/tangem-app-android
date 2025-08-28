package com.tangem.features.hotwallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface ViewPhraseComponent : ComposableContentComponent {

    data class Params(
        val words: List<String>,
    )

    interface Factory : ComponentFactory<Params, ViewPhraseComponent>
}