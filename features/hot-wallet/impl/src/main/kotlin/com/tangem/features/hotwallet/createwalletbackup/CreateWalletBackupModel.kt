package com.tangem.features.hotwallet.createwalletbackup

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.navigation.popTo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.hotwallet.CreateWalletBackupComponent
import com.tangem.features.hotwallet.createwalletbackup.routing.CreateWalletBackupRoute
import com.tangem.features.hotwallet.manualbackup.check.ManualBackupCheckComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.manualbackup.phrase.ManualBackupPhraseComponent
import com.tangem.features.hotwallet.manualbackup.start.ManualBackupStartComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ModelScoped
internal class CreateWalletBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val trackingContextProxy: TrackingContextProxy,
) : Model() {

    val params = paramsContainer.require<CreateWalletBackupComponent.Params>()

    val manualBackupStartModelCallbacks = ManualBackupStartModelCallbacks()
    val manualBackupPhraseModelCallbacks = ManualBackupPhraseModelCallbacks()
    val manualBackupCheckModelCallbacks = ManualBackupCheckModelCallbacks()
    val manualBackupCompletedModelCallbacks = ManualBackupCompletedModelCallbacks()

    val stackNavigation = StackNavigation<CreateWalletBackupRoute>()
    val startRoute = CreateWalletBackupRoute.RecoveryPhraseStart
    val currentRoute: MutableStateFlow<CreateWalletBackupRoute> = MutableStateFlow(startRoute)

    init {
        trackingContextProxy.addHotWalletContext()
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingContextProxy.removeContext()
    }

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
        stackNavigation.push(CreateWalletBackupRoute.BackupCompleted(isUpgradeFlow = params.isUpgradeFlow))
    }

    fun onManualBackupCompleted() {
        if (params.setAccessCode) {
            router.replaceCurrent(
                route = AppRoute.UpdateAccessCode(
                    userWalletId = params.userWalletId,
                    isFirstSetup = true,
                ),
            )
        } else {
            router.pop()
        }
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

        override fun onUpgradeClick(userWalletId: UserWalletId) {
            router.popTo<AppRoute.WalletSettings>()
            router.push(
                AppRoute.UpgradeWallet(
                    userWalletId = userWalletId,
                ),
            )
        }
    }
}