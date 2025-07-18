package com.tangem.features.hotwallet.addexistingwallet.root.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.hotwallet.setaccesscode.SetAccessCodeComponent
import com.tangem.features.hotwallet.addexistingwallet.root.AddExistingWalletModel
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartComponent
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import javax.inject.Inject

internal class AddExistingWalletChildFactory @Inject constructor(
    private val pushNotificationsComponent: PushNotificationsComponent.Factory,
) {

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
            is AddExistingWalletRoute.Import -> AddExistingWalletImportComponent(
                context = childContext,
                params = AddExistingWalletImportComponent.Params(
                    callbacks = model.addExistingWalletImportModelCallbacks,
                ),
            )
            is AddExistingWalletRoute.PushNotifications -> pushNotificationsComponent.create(
                context = childContext,
                params = PushNotificationsComponent.Params.Callbacks(
                    callbacks = model.pushNotificationsComponentModelCallbacks,
                ),
            )
            is AddExistingWalletRoute.AccessCode -> SetAccessCodeComponent(
                context = childContext,
                params = SetAccessCodeComponent.Params(
                    callbacks = model.accessCodeModelCallbacks,
                ),
            )
        }
    }
}