package com.tangem.features.onboarding.usedcard.entry

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.onboarding.usedcard.UsedCardOnboardingComponent
import com.tangem.features.onboarding.usedcard.alreadyactivated.AlreadyActivatedComponent
import com.tangem.features.onboarding.usedcard.routing.UsedCardOnboardingRoute
import com.tangem.features.pushnotifications.api.PushNotificationsModelCallbacks
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class UsedCardOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val appRouter: AppRouter,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
) : Model() {

    private val params = paramsContainer.require<UsedCardOnboardingComponent.Params>()

    val scanResponse = params.scanResponse
    val alreadyActivatedCallback = AlreadyActivatedCallback()
    val askBiometryCallbacks = AskBiometryCallbacks()
    val pushNotificationsCallbacks = PushNotificationsCallbacks()
    val syncWalletCallbacks = SyncWalletCallbacks()

    val stackNavigation = StackNavigation<UsedCardOnboardingRoute>()
    val startRoute: UsedCardOnboardingRoute = UsedCardOnboardingRoute.AlreadyActivated
    val currentRoute: MutableStateFlow<UsedCardOnboardingRoute> = MutableStateFlow(startRoute)

    private var shouldShowBiometry: Boolean = false
    private var shouldShowPushNotifications: Boolean = false

    init {
        resolveAvailableSteps()
    }

    private fun resolveAvailableSteps() {
        modelScope.launch {
            val canBiometry = canUseBiometryUseCase() && shouldShowAskBiometryUseCase()
            val canPush = shouldAskPermissionUseCase(PUSH_PERMISSION)
            shouldShowBiometry = canBiometry
            shouldShowPushNotifications = canPush
        }
    }

    fun onBackClick() {
        when (currentRoute.value) {
            is UsedCardOnboardingRoute.AlreadyActivated -> router.pop()
            else -> Unit
        }
    }

    private fun navigateAfterAlreadyActivated() {
        modelScope.launch {
            when {
                shouldShowBiometry -> {
                    stackNavigation.replaceAll(UsedCardOnboardingRoute.AskBiometry)
                }
                shouldShowPushNotifications -> {
                    stackNavigation.replaceAll(UsedCardOnboardingRoute.PushNotifications)
                }
                else -> {
                    stackNavigation.replaceAll(UsedCardOnboardingRoute.SyncWallet)
                }
            }
        }
    }

    private fun navigateAfterBiometry() {
        modelScope.launch {
            if (shouldShowPushNotifications) {
                stackNavigation.replaceAll(UsedCardOnboardingRoute.PushNotifications)
            } else {
                stackNavigation.replaceAll(UsedCardOnboardingRoute.SyncWallet)
            }
        }
    }

    private fun navigateToSyncWallet() {
        stackNavigation.replaceAll(UsedCardOnboardingRoute.SyncWallet)
    }

    inner class AlreadyActivatedCallback : AlreadyActivatedComponent.ModelCallback {
        override fun onWalletSaved() {
            navigateAfterAlreadyActivated()
        }
    }

    inner class AskBiometryCallbacks : AskBiometryComponent.ModelCallbacks {
        override fun onAllowed() {
            navigateAfterBiometry()
        }

        override fun onDenied() {
            navigateAfterBiometry()
        }
    }

    inner class PushNotificationsCallbacks : PushNotificationsModelCallbacks {
        override fun onAllowSystemPermission() {
            navigateToSyncWallet()
        }

        override fun onDenySystemPermission() {
            navigateToSyncWallet()
        }

        override fun onDismiss() {
            navigateToSyncWallet()
        }
    }

    inner class SyncWalletCallbacks {
        fun onContinueClick() {
            appRouter.replaceAll(AppRoute.Wallet)
        }
    }
}