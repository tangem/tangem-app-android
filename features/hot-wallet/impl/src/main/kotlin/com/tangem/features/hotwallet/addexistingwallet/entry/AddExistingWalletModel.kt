package com.tangem.features.hotwallet.addexistingwallet.entry

import com.arkivanov.decompose.router.stack.*
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.features.hotwallet.addexistingwallet.entry.routing.AddExistingWalletRoute
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.setaccesscode.AccessCodeComponent
import com.tangem.features.hotwallet.setupfinished.MobileWalletSetupFinishedComponent
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
    val accessCodeModelCallbacks = AccessCodeModelCallbacks()
    val mobileWalletSetupFinishedComponentModelCallbacks = MobileWalletSetupFinishedComponentModelCallbacks()

    val stackNavigation = StackNavigation<AddExistingWalletRoute>()

    fun onChildBack(currentRoute: AddExistingWalletRoute) {
        when (currentRoute) {
            is AddExistingWalletRoute.Import -> stackNavigation.pop()
            is AddExistingWalletRoute.BackupCompleted -> Unit
            is AddExistingWalletRoute.SetAccessCode -> Unit
            is AddExistingWalletRoute.ConfirmAccessCode -> stackNavigation.pop()
            is AddExistingWalletRoute.PushNotifications -> Unit
            is AddExistingWalletRoute.SetupFinished -> Unit
            is AddExistingWalletRoute.Start -> Unit
        }
    }

    fun onSkipAccessCode() {
        navigateToPushNotificationsOrNext()
    }

    private fun navigateToPushNotificationsOrNext() {
        modelScope.launch {
            val shouldRequestPush = shouldAskPermissionUseCase(PUSH_PERMISSION)
            if (shouldRequestPush) {
                // is yet blocked by [REDACTED_TASK_KEY]
                // stackNavigation.replaceAll(AddExistingWalletRoute.PushNotifications)
                stackNavigation.replaceAll(AddExistingWalletRoute.SetupFinished)
            } else {
                stackNavigation.replaceAll(AddExistingWalletRoute.SetupFinished)
            }
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
        override fun onWalletImported(userWalletId: UserWalletId) {
            stackNavigation.replaceCurrent(AddExistingWalletRoute.BackupCompleted(userWalletId))
        }
    }

    inner class ManualBackupCompletedComponentModelCallbacks : ManualBackupCompletedComponent.ModelCallbacks {
        override fun onContinueClick(userWalletId: UserWalletId) {
            stackNavigation.replaceCurrent(AddExistingWalletRoute.SetAccessCode(userWalletId))
        }
    }

    inner class AccessCodeModelCallbacks : AccessCodeComponent.ModelCallbacks {
        override fun onAccessCodeSet(userWalletId: UserWalletId, accessCode: String) {
            stackNavigation.push(AddExistingWalletRoute.ConfirmAccessCode(userWalletId, accessCode))
        }

        override fun onAccessCodeConfirmed(userWalletId: UserWalletId) {
            navigateToPushNotificationsOrNext()
        }
    }

    inner class MobileWalletSetupFinishedComponentModelCallbacks :
        MobileWalletSetupFinishedComponent.ModelCallbacks {
        override fun onContinueClick() {
            router.replaceAll(AppRoute.Wallet)
        }
    }
}