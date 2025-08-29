package com.tangem.features.hotwallet.createwalletbackup

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.hotwallet.CreateWalletBackupComponent
import com.tangem.features.hotwallet.createwalletbackup.routing.CreateWalletBackupRoute
import com.tangem.features.hotwallet.manualbackup.check.ManualBackupCheckComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.manualbackup.phrase.ManualBackupPhraseComponent
import com.tangem.features.hotwallet.manualbackup.start.ManualBackupStartComponent
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ModelScoped
internal class CreateWalletBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    val params = paramsContainer.require<CreateWalletBackupComponent.Params>()

    val hotWalletStepperComponentModelCallback = HotWalletStepperComponentModelCallback()
    val manualBackupStartModelCallbacks = ManualBackupStartModelCallbacks()
    val manualBackupPhraseModelCallbacks = ManualBackupPhraseModelCallbacks()
    val manualBackupCheckModelCallbacks = ManualBackupCheckModelCallbacks()
    val manualBackupCompletedModelCallbacks = ManualBackupCompletedModelCallbacks()

    val stackNavigation = StackNavigation<CreateWalletBackupRoute>()
    val startRoute = CreateWalletBackupRoute.RecoveryPhraseStart
    val currentRoute: MutableStateFlow<CreateWalletBackupRoute> = MutableStateFlow(startRoute)

    fun onBack() {
        when (currentRoute.value) {
            is CreateWalletBackupRoute.RecoveryPhraseStart -> router.pop()
            is CreateWalletBackupRoute.RecoveryPhrase -> stackNavigation.pop()
            is CreateWalletBackupRoute.ConfirmBackup -> stackNavigation.pop()
            is CreateWalletBackupRoute.BackupCompleted -> router.pop()
        }
    }

    fun onManualBackupStarted() {
        stackNavigation.push(CreateWalletBackupRoute.RecoveryPhrase)
    }

    fun onManualBackupPhraseShown() {
        stackNavigation.push(CreateWalletBackupRoute.ConfirmBackup)
    }

    fun onManualBackupChecked() {
        stackNavigation.push(CreateWalletBackupRoute.BackupCompleted)
    }

    fun onManualBackupCompleted() {
        router.pop()
    }

    inner class HotWalletStepperComponentModelCallback : HotWalletStepperComponent.ModelCallback {
        override fun onBackClick() {
            onBack()
        }

        override fun onSkipClick() = Unit
    }

    inner class ManualBackupStartModelCallbacks : ManualBackupStartComponent.ModelCallbacks {
        override fun onContinueClick() {
            onManualBackupStarted()
        }
    }

    inner class ManualBackupPhraseModelCallbacks : ManualBackupPhraseComponent.ModelCallbacks {
        override fun onContinueClick() {
            onManualBackupPhraseShown()
        }
    }

    inner class ManualBackupCheckModelCallbacks : ManualBackupCheckComponent.ModelCallbacks {
        override fun onCompleteClick() {
            onManualBackupChecked()
        }
    }

    inner class ManualBackupCompletedModelCallbacks : ManualBackupCompletedComponent.ModelCallbacks {
        override fun onContinueClick(userWalletId: UserWalletId) {
            onManualBackupCompleted()
        }
    }
}