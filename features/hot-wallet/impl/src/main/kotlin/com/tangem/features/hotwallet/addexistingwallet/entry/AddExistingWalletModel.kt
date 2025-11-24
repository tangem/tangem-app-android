package com.tangem.features.hotwallet.addexistingwallet.entry

import com.arkivanov.decompose.router.stack.*
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.hotwallet.SetAccessCodeSkippedUseCase
import com.tangem.features.hotwallet.addexistingwallet.entry.routing.AddExistingWalletRoute
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.accesscode.AccessCodeComponent
import com.tangem.features.hotwallet.setupfinished.MobileWalletSetupFinishedComponent
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.features.pushnotifications.api.PushNotificationsModelCallbacks
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class AddExistingWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val trackingContextProxy: TrackingContextProxy,
    private val setAccessCodeSkippedUseCase: SetAccessCodeSkippedUseCase,
) : Model() {

    val hotWalletStepperComponentModelCallback = HotWalletStepperComponentModelCallback()
    val addExistingWalletImportModelCallbacks = AddExistingWalletImportModelCallbacks()
    val manualBackupCompletedModelCallbacks = ManualBackupCompletedModelCallbacks()
    val accessCodeModelCallbacks = AccessCodeModelCallbacks()
    val pushNotificationsCallbacks = PushNotificationsCallbacks()
    val mobileWalletSetupFinishedModelCallbacks = MobileWalletSetupFinishedModelCallbacks()

    val stackNavigation = StackNavigation<AddExistingWalletRoute>()
    val startRoute = AddExistingWalletRoute.Import
    val currentRoute: MutableStateFlow<AddExistingWalletRoute> = MutableStateFlow(startRoute)

    init {
        trackingContextProxy.addHotWalletContext()
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingContextProxy.removeContext()
    }

    fun onChildBack() {
        when (currentRoute.value) {
            is AddExistingWalletRoute.Import -> router.pop()
            is AddExistingWalletRoute.BackupCompleted -> Unit
            is AddExistingWalletRoute.SetAccessCode -> Unit
            is AddExistingWalletRoute.ConfirmAccessCode -> stackNavigation.pop()
            is AddExistingWalletRoute.PushNotifications -> Unit
            is AddExistingWalletRoute.SetupFinished -> Unit
        }
    }

    private fun navigateToPushNotificationsOrNext() {
        modelScope.launch {
            val shouldRequestPush = shouldAskPermissionUseCase(PUSH_PERMISSION)
            if (shouldRequestPush) {
                stackNavigation.replaceAll(AddExistingWalletRoute.PushNotifications)
            } else {
                stackNavigation.replaceAll(AddExistingWalletRoute.SetupFinished)
            }
        }
    }

    private fun navigateToSetupFinished() {
        stackNavigation.replaceAll(AddExistingWalletRoute.SetupFinished)
    }

    private fun showSkipAccessCodeWarningDialog() {
        val userWalletId = when (val route = currentRoute.value) {
            is AddExistingWalletRoute.SetAccessCode -> route.userWalletId
            is AddExistingWalletRoute.ConfirmAccessCode -> route.userWalletId
            else -> null
        }

        uiMessageSender.send(
            DialogMessage(
                message = resourceReference(R.string.access_code_alert_skip_description),
                title = resourceReference(R.string.access_code_alert_skip_title),
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.common_cancel),
                    onClick = {},
                ),
                secondAction = EventMessageAction(
                    title = resourceReference(R.string.access_code_alert_skip_ok),
                    onClick = {
                        if (userWalletId != null) {
                            modelScope.launch {
                                setAccessCodeSkippedUseCase(userWalletId, true)
                            }
                        }
                        navigateToPushNotificationsOrNext()
                    },
                ),
                shouldDismissOnFirstAction = true,
            ),
        )
    }

    inner class HotWalletStepperComponentModelCallback : HotWalletStepperComponent.ModelCallback {
        override fun onBackClick() {
            onChildBack()
        }

        override fun onSkipClick() {
            showSkipAccessCodeWarningDialog()
        }
    }

    inner class AddExistingWalletImportModelCallbacks : AddExistingWalletImportComponent.ModelCallbacks {
        override fun onWalletImported(userWalletId: UserWalletId) {
            stackNavigation.replaceAll(AddExistingWalletRoute.BackupCompleted(userWalletId))
        }
    }

    inner class ManualBackupCompletedModelCallbacks : ManualBackupCompletedComponent.ModelCallbacks {
        override fun onContinueClick(userWalletId: UserWalletId) {
            stackNavigation.replaceAll(AddExistingWalletRoute.SetAccessCode(userWalletId))
        }

        override fun onUpgradeClick(userWalletId: UserWalletId) = Unit
    }

    inner class AccessCodeModelCallbacks : AccessCodeComponent.ModelCallbacks {
        override fun onNewAccessCodeInput(userWalletId: UserWalletId, accessCode: String) {
            stackNavigation.push(AddExistingWalletRoute.ConfirmAccessCode(userWalletId, accessCode))
        }

        override fun onAccessCodeUpdated(userWalletId: UserWalletId) {
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

    inner class MobileWalletSetupFinishedModelCallbacks :
        MobileWalletSetupFinishedComponent.ModelCallbacks {
        override fun onFinishClick() {
            router.replaceAll(AppRoute.Wallet)
        }
    }
}