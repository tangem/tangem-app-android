package com.tangem.features.hotwallet.addexistingwallet.entry.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.hotwallet.accesscode.set.SetAccessCodeComponent
import com.tangem.features.hotwallet.accesscode.confirm.ConfirmAccessCodeComponent
import com.tangem.features.hotwallet.addexistingwallet.entry.AddExistingWalletModel
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartComponent
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.setupfinished.MobileWalletSetupFinishedComponent
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import javax.inject.Inject

internal class AddExistingWalletChildFactory @Inject constructor(
    private val pushNotificationsComponent: PushNotificationsComponent.Factory,
    private val setAccessCodeSetComponentFactory: SetAccessCodeComponent.Factory,
    private val confirmAccessCodeComponentFactory: ConfirmAccessCodeComponent.Factory,
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
            is AddExistingWalletRoute.BackupCompleted -> ManualBackupCompletedComponent(
                context = childContext,
                params = ManualBackupCompletedComponent.Params(
                    callbacks = model.manualBackupCompletedComponentModelCallbacks,
                ),
            )
            is AddExistingWalletRoute.SetAccessCode -> setAccessCodeSetComponentFactory.create(
                context = childContext,
                params = SetAccessCodeComponent.Params(
                    callbacks = model.setAccessCodeModelCallbacks,
                ),
            )
            is AddExistingWalletRoute.ConfirmAccessCode -> confirmAccessCodeComponentFactory.create(
                context = childContext,
                params = ConfirmAccessCodeComponent.Params(
                    accessCodeToConfirm = route.accessCode,
                    callbacks = model.confirmAccessCodeModelCallbacks,
                ),
            )
            is AddExistingWalletRoute.PushNotifications -> pushNotificationsComponent.create(
                context = childContext,
                params = PushNotificationsComponent.Params.Callbacks(
                    callbacks = model.pushNotificationsComponentModelCallbacks,
                ),
            )
            is AddExistingWalletRoute.SetupFinished -> MobileWalletSetupFinishedComponent(
                context = childContext,
                params = MobileWalletSetupFinishedComponent.Params(
                    callbacks = model.mobileWalletSetupFinishedComponentModelCallbacks,
                ),
            )
        }
    }
}