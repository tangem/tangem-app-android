package com.tangem.features.hotwallet.updateaccesscode.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.hotwallet.updateaccesscode.UpdateAccessCodeModel
import com.tangem.features.hotwallet.accesscode.AccessCodeComponent
import javax.inject.Inject

internal class UpdateAccessCodeChildFactory @Inject constructor(
    private val accessCodeComponentFactory: AccessCodeComponent.Factory,
) {

    fun createChild(
        route: UpdateAccessCodeRoute,
        childContext: AppComponentContext,
        model: UpdateAccessCodeModel,
    ): ComposableContentComponent {
        return when (route) {
            is UpdateAccessCodeRoute.SetAccessCode -> accessCodeComponentFactory.create(
                context = childContext,
                params = AccessCodeComponent.Params(
                    userWalletId = route.userWalletId,
                    callbacks = model,
                ),
            )
            is UpdateAccessCodeRoute.ConfirmAccessCode -> accessCodeComponentFactory.create(
                context = childContext,
                params = AccessCodeComponent.Params(
                    accessCodeToConfirm = route.accessCode,
                    userWalletId = route.userWalletId,
                    callbacks = model,
                ),
            )
        }
    }
}