package com.tangem.features.onboarding.usedcard.routing

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.onboarding.usedcard.entry.UsedCardOnboardingModel
import com.tangem.features.onboarding.usedcard.alreadyactivated.AlreadyActivatedComponent
import com.tangem.features.onboarding.usedcard.syncwallet.SyncWalletComponent
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import javax.inject.Inject

internal class UsedCardOnboardingChildFactory @Inject constructor(
    private val askBiometryComponentFactory: AskBiometryComponent.Factory,
    private val pushNotificationsComponentFactory: PushNotificationsComponent.Factory,
) {

    fun createChild(
        route: UsedCardOnboardingRoute,
        childContext: AppComponentContext,
        model: UsedCardOnboardingModel,
    ): ComposableContentComponent {
        return when (route) {
            is UsedCardOnboardingRoute.AlreadyActivated -> AlreadyActivatedComponent(
                context = childContext,
                params = AlreadyActivatedComponent.Params(
                    onThisIsMyWalletClick = model.alreadyActivatedCallbacks::onThisIsMyWalletClick,
                    onNewCardClick = model.alreadyActivatedCallbacks::onNewCardClick,
                ),
            )
            is UsedCardOnboardingRoute.AskBiometry -> askBiometryComponentFactory.create(
                context = childContext,
                params = AskBiometryComponent.Params(
                    isBottomSheetVariant = false,
                    modelCallbacks = model.askBiometryCallbacks,
                ),
            )
            is UsedCardOnboardingRoute.PushNotifications -> pushNotificationsComponentFactory.create(
                context = childContext,
                params = PushNotificationsParams(
                    modelCallbacks = model.pushNotificationsCallbacks,
                    source = AppRoute.PushNotification.Source.Onboarding,
                ),
            )
            is UsedCardOnboardingRoute.SyncWallet -> SyncWalletComponent(
                context = childContext,
                params = SyncWalletComponent.Params(
                    onContinueClick = model.syncWalletCallbacks::onContinueClick,
                ),
            )
        }
    }
}