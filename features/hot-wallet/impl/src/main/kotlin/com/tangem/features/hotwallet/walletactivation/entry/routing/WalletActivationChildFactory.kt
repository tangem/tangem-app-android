package com.tangem.features.hotwallet.walletactivation.entry.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.hotwallet.manualbackup.check.ManualBackupCheckComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.manualbackup.phrase.ManualBackupPhraseComponent
import com.tangem.features.hotwallet.manualbackup.start.ManualBackupStartComponent
import com.tangem.features.hotwallet.setaccesscode.AccessCodeComponent
import com.tangem.features.hotwallet.setupfinished.MobileWalletSetupFinishedComponent
import com.tangem.features.hotwallet.walletactivation.entry.WalletActivationModel
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import javax.inject.Inject

internal class WalletActivationChildFactory @Inject constructor(
    private val pushNotificationsComponent: PushNotificationsComponent.Factory,
    private val accessCodeComponentFactory: AccessCodeComponent.Factory,
) {

    fun createChild(
        route: WalletActivationRoute,
        childContext: AppComponentContext,
        model: WalletActivationModel,
    ): ComposableContentComponent {
        return when (route) {
            is WalletActivationRoute.ManualBackupStart -> ManualBackupStartComponent(
                context = childContext,
                params = ManualBackupStartComponent.Params(
                    callbacks = model.manualBackupStartModelCallbacks,
                ),
            )
            is WalletActivationRoute.ManualBackupPhrase -> ManualBackupPhraseComponent(
                context = childContext,
                params = ManualBackupPhraseComponent.Params(
                    userWalletId = model.params.userWalletId,
                    callbacks = model.manualBackupPhraseModelCallbacks,
                ),
            )
            is WalletActivationRoute.ManualBackupCheck -> ManualBackupCheckComponent(
                context = childContext,
                params = ManualBackupCheckComponent.Params(
                    userWalletId = model.params.userWalletId,
                    callbacks = model.manualBackupCheckModelCallbacks,
                ),
            )
            is WalletActivationRoute.ManualBackupCompleted -> ManualBackupCompletedComponent(
                context = childContext,
                params = ManualBackupCompletedComponent.Params(
                    userWalletId = model.params.userWalletId,
                    callbacks = model.manualBackupCompletedModelCallbacks,
                ),
            )
            is WalletActivationRoute.SetAccessCode -> accessCodeComponentFactory.create(
                context = childContext,
                params = AccessCodeComponent.Params(
                    isConfirmMode = false,
                    userWalletId = model.params.userWalletId,
                    callbacks = model.accessCodeModelCallbacks,
                ),
            )
            is WalletActivationRoute.ConfirmAccessCode -> accessCodeComponentFactory.create(
                context = childContext,
                params = AccessCodeComponent.Params(
                    isConfirmMode = true,
                    accessCodeToConfirm = route.accessCode,
                    userWalletId = model.params.userWalletId,
                    callbacks = model.accessCodeModelCallbacks,
                ),
            )
            is WalletActivationRoute.PushNotifications -> pushNotificationsComponent.create(
                context = childContext,
                params = PushNotificationsParams(
                    modelCallbacks = model.pushNotificationsCallbacks,
                ),
            )
            is WalletActivationRoute.SetupFinished -> MobileWalletSetupFinishedComponent(
                context = childContext,
                params = MobileWalletSetupFinishedComponent.Params(
                    callbacks = model.mobileWalletSetupFinishedModelCallbacks,
                ),
            )
        }
    }
}