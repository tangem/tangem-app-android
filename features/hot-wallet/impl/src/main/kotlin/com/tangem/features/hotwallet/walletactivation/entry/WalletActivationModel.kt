package com.tangem.features.hotwallet.walletactivation.entry

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.features.hotwallet.manualbackup.check.ManualBackupCheckComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.manualbackup.phrase.ManualBackupPhraseComponent
import com.tangem.features.hotwallet.manualbackup.start.ManualBackupStartComponent
import com.tangem.features.hotwallet.accesscode.AccessCodeComponent
import com.tangem.features.hotwallet.setupfinished.MobileWalletSetupFinishedComponent
import com.tangem.features.hotwallet.walletactivation.entry.routing.WalletActivationRoute
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.hotwallet.WalletActivationComponent
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.features.pushnotifications.api.PushNotificationsModelCallbacks
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class WalletActivationModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
) : Model() {

    val params = paramsContainer.require<WalletActivationComponent.Params>()

    val hotWalletStepperComponentModelCallback = HotWalletStepperComponentModelCallback()
    val manualBackupStartModelCallbacks = ManualBackupStartModelCallbacks()
    val manualBackupPhraseModelCallbacks = ManualBackupPhraseModelCallbacks()
    val manualBackupCheckModelCallbacks = ManualBackupCheckModelCallbacks()
    val manualBackupCompletedModelCallbacks = ManualBackupCompletedModelCallbacks()
    val accessCodeModelCallbacks = AccessCodeModelCallbacks()
    val pushNotificationsCallbacks = PushNotificationsCallbacks()
    val mobileWalletSetupFinishedModelCallbacks = MobileWalletSetupFinishedModelCallbacks()

    val stackNavigation = StackNavigation<WalletActivationRoute>()
    val startRoute = WalletActivationRoute.ManualBackupStart
    val currentRoute: MutableStateFlow<WalletActivationRoute> = MutableStateFlow(startRoute)

    fun onChildBack() {
        when (currentRoute.value) {
            is WalletActivationRoute.ManualBackupStart -> router.pop()
            is WalletActivationRoute.ManualBackupPhrase -> stackNavigation.pop()
            is WalletActivationRoute.ManualBackupCheck -> stackNavigation.pop()
            is WalletActivationRoute.ManualBackupCompleted -> Unit
            is WalletActivationRoute.SetAccessCode -> Unit
            is WalletActivationRoute.ConfirmAccessCode -> Unit
            is WalletActivationRoute.PushNotifications -> Unit
            is WalletActivationRoute.SetupFinished -> Unit
        }
    }

    private fun navigateToPushNotificationsOrNext() {
        modelScope.launch {
            val shouldRequestPush = shouldAskPermissionUseCase(PUSH_PERMISSION)
            if (shouldRequestPush) {
                // is yet blocked by [REDACTED_TASK_KEY]
                // stackNavigation.replaceAll(AddExistingWalletRoute.PushNotifications)
                stackNavigation.replaceAll(WalletActivationRoute.SetupFinished)
            } else {
                stackNavigation.replaceAll(WalletActivationRoute.SetupFinished)
            }
        }
    }

    private fun navigateToSetupFinished() {
        stackNavigation.replaceAll(WalletActivationRoute.SetupFinished)
    }

    inner class HotWalletStepperComponentModelCallback : HotWalletStepperComponent.ModelCallback {
        override fun onBackClick() {
            onChildBack()
        }

        override fun onSkipClick() {
            navigateToPushNotificationsOrNext()
        }
    }

    inner class ManualBackupStartModelCallbacks : ManualBackupStartComponent.ModelCallbacks {
        override fun onContinueClick() {
            stackNavigation.push(WalletActivationRoute.ManualBackupPhrase)
        }
    }

    inner class ManualBackupPhraseModelCallbacks : ManualBackupPhraseComponent.ModelCallbacks {
        override fun onContinueClick() {
            stackNavigation.push(
                WalletActivationRoute.ManualBackupCheck,
            )
        }
    }

    inner class ManualBackupCheckModelCallbacks : ManualBackupCheckComponent.ModelCallbacks {
        override fun onCompleteClick() {
            stackNavigation.push(WalletActivationRoute.ManualBackupCompleted)
        }
    }

    inner class ManualBackupCompletedModelCallbacks : ManualBackupCompletedComponent.ModelCallbacks {
        override fun onContinueClick(userWalletId: UserWalletId) {
            stackNavigation.push(WalletActivationRoute.SetAccessCode)
        }
    }

    inner class AccessCodeModelCallbacks : AccessCodeComponent.ModelCallbacks {
        override fun onAccessCodeSet(userWalletId: UserWalletId, accessCode: String) {
            stackNavigation.push(WalletActivationRoute.ConfirmAccessCode(accessCode))
        }

        override fun onAccessCodeConfirmed(userWalletId: UserWalletId) {
            navigateToPushNotificationsOrNext()
        }
    }

    inner class PushNotificationsCallbacks : PushNotificationsModelCallbacks {
        override fun onAllowSystemPermission() {
            navigateToSetupFinished()
        }

        override fun onDenySystemPermission() {
            navigateToSetupFinished()
        }

        override fun onDismiss() {
            navigateToSetupFinished()
        }
    }

    inner class MobileWalletSetupFinishedModelCallbacks : MobileWalletSetupFinishedComponent.ModelCallbacks {
        override fun onContinueClick() {
            router.pop()
        }
    }
}