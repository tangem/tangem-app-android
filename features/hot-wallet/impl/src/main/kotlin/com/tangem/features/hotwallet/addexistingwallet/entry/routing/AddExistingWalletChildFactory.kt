package com.tangem.features.hotwallet.addexistingwallet.entry.routing

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.hotwallet.addexistingwallet.entry.AddExistingWalletModel
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.accesscode.AccessCodeComponent
import com.tangem.features.hotwallet.setupfinished.MobileWalletSetupFinishedComponent
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import javax.inject.Inject

internal class AddExistingWalletChildFactory @Inject constructor(
    private val pushNotificationsComponent: PushNotificationsComponent.Factory,
    private val accessCodeComponentFactory: AccessCodeComponent.Factory,
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
                    userWalletId = route.userWalletId,
                    callbacks = model.manualBackupCompletedComponentModelCallbacks,
                ),
            )
            is AddExistingWalletRoute.SetAccessCode -> accessCodeComponentFactory.create(
                context = childContext,
                params = AccessCodeComponent.Params(
                    isConfirmMode = false,
                    userWalletId = route.userWalletId,
                    callbacks = model.accessCodeModelCallbacks,
                ),
            )
            is AddExistingWalletRoute.ConfirmAccessCode -> accessCodeComponentFactory.create(
                context = childContext,
                params = AccessCodeComponent.Params(
                    isConfirmMode = true,
                    accessCodeToConfirm = route.accessCode,
                    userWalletId = route.userWalletId,
                    callbacks = model.accessCodeModelCallbacks,
                ),
            )
            is AddExistingWalletRoute.PushNotifications -> pushNotificationsComponent.create(
                context = childContext,
                params = PushNotificationsParams(
                    modelCallbacks = model.pushNotificationsCallbacks,
                    source = AppRoute.PushNotification.Source.Onboarding,
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