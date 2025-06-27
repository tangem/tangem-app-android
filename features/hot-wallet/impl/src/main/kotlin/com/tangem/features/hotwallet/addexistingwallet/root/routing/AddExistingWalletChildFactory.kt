package com.tangem.features.hotwallet.addexistingwallet.root.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.hotwallet.addexistingwallet.root.AddExistingWalletModel
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartComponent
import javax.inject.Inject

internal class AddExistingWalletChildFactory @Inject constructor() {

    fun createChild(
        route: AddExistingWalletRoute,
        childContext: AppComponentContext,
        model: AddExistingWalletModel,
    ): ComposableContentComponent {
        return when (route) {
            is AddExistingWalletRoute.Start -> AddExistingWalletStartComponent(
                context = childContext,
                params = AddExistingWalletStartComponent.Params(
                    callbacks = model.addExistingWalletStartModelCallbacks,
                ),
            )
        }
    }
}