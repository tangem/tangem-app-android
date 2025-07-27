package com.tangem.features.hotwallet.addexistingwallet.root

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.hotwallet.addexistingwallet.root.routing.AddExistingWalletRoute
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.accesscode.confirm.ConfirmAccessCodeComponent
import com.tangem.features.hotwallet.accesscode.set.SetAccessCodeComponent
import com.tangem.features.hotwallet.setupfinished.MobileWalletSetupFinishedComponent
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class AddExistingWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
) : Model() {

    val addExistingWalletStartModelCallbacks = AddExistingWalletStartModelCallbacks()
    val addExistingWalletImportModelCallbacks = AddExistingWalletImportModelCallbacks()
    val manualBackupCompletedComponentModelCallbacks = ManualBackupCompletedComponentModelCallbacks()
    val pushNotificationsComponentModelCallbacks = PushNotificationsComponentModelCallbacks()
    val setAccessCodeModelCallbacks = SetAccessCodeModelCallbacks()
    val confirmAccessCodeModelCallbacks = ConfirmAccessCodeModelCallbacks()
    val mobileWalletSetupFinishedComponentModelCallbacks = MobileWalletSetupFinishedComponentModelCallbacks()

    val stackNavigation = StackNavigation<AddExistingWalletRoute>()

    fun onChildBack(currentRoute: AddExistingWalletRoute) {
        when (currentRoute) {
            is AddExistingWalletRoute.Import -> stackNavigation.pop()
            is AddExistingWalletRoute.BackupCompleted -> Unit
            is AddExistingWalletRoute.ConfirmAccessCode -> stackNavigation.pop()
            is AddExistingWalletRoute.SetAccessCode -> stackNavigation.pop()
            is AddExistingWalletRoute.PushNotifications -> Unit
            is AddExistingWalletRoute.SetupFinished -> Unit
            is AddExistingWalletRoute.Start -> Unit
        }
    }

    inner class AddExistingWalletStartModelCallbacks : AddExistingWalletStartComponent.ModelCallbacks {
        override fun onBackClick() {
            router.pop()
        }

        override fun onImportPhraseClick() {
            stackNavigation.push(AddExistingWalletRoute.Import)
        }
    }

    inner class AddExistingWalletImportModelCallbacks : AddExistingWalletImportComponent.ModelCallbacks {
        override fun onWalletImported() {
            stackNavigation.replaceCurrent(AddExistingWalletRoute.BackupCompleted)
        }
    }

    inner class ManualBackupCompletedComponentModelCallbacks : ManualBackupCompletedComponent.ModelCallbacks {
        override fun onContinueClick() {
            stackNavigation.push(AddExistingWalletRoute.SetAccessCode)
        }
    }

    inner class SetAccessCodeModelCallbacks : SetAccessCodeComponent.ModelCallbacks {
        override fun onAccessCodeSet(accessCode: String) {
            stackNavigation.push(AddExistingWalletRoute.ConfirmAccessCode(accessCode))
        }
    }

    inner class ConfirmAccessCodeModelCallbacks : ConfirmAccessCodeComponent.ModelCallbacks {
        override fun onAccessCodeConfirmed() {
            modelScope.launch {
                val shouldRequestPush = shouldAskPermissionUseCase(PUSH_PERMISSION)
                if (shouldRequestPush) {
                    // is yet blocked by [REDACTED_TASK_KEY]
                    // stackNavigation.replaceCurrent(AddExistingWalletRoute.PushNotifications)
                    stackNavigation.replaceCurrent(AddExistingWalletRoute.SetupFinished)
                } else {
                    stackNavigation.replaceCurrent(AddExistingWalletRoute.SetupFinished)
                }
            }
        }
    }

    inner class PushNotificationsComponentModelCallbacks : PushNotificationsComponent.ModelCallbacks {
        override fun onResult() {
            stackNavigation.replaceCurrent(AddExistingWalletRoute.SetupFinished)
        }
    }

    inner class MobileWalletSetupFinishedComponentModelCallbacks :
        MobileWalletSetupFinishedComponent.ModelCallbacks {
        override fun onContinueClick() {
            router.replaceAll(AppRoute.Wallet)
        }
    }
}