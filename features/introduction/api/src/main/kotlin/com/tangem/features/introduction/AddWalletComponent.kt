package com.tangem.features.introduction

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

/** Its content covers the whole host screen: a social sign-in shows full-screen progress and consent. */
interface AddWalletComponent : ComposableContentComponent {

    fun open()

    data class Params(val callbacks: ModelCallbacks)

    interface ModelCallbacks {

        /** The sheet is already dismissed and the tap reported */
        fun onAddCardClick()
    }

    interface Factory : ComponentFactory<Params, AddWalletComponent>
}