package com.tangem.features.hotwallet.createwalletbackup.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.hotwallet.createwalletbackup.CreateWalletBackupModel
import com.tangem.features.hotwallet.manualbackup.check.ManualBackupCheckComponent
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedComponent
import com.tangem.features.hotwallet.manualbackup.phrase.ManualBackupPhraseComponent
import com.tangem.features.hotwallet.manualbackup.start.ManualBackupStartComponent
import javax.inject.Inject

internal class CreateWalletBackupChildFactory @Inject constructor() {

    fun createChild(
        route: CreateWalletBackupRoute,
        childContext: AppComponentContext,
        model: CreateWalletBackupModel,
    ): ComposableContentComponent = when (route) {
        CreateWalletBackupRoute.RecoveryPhraseStart -> ManualBackupStartComponent(
            context = childContext,
            params = ManualBackupStartComponent.Params(
                callbacks = model.manualBackupStartModelCallbacks,
            ),
        )
        CreateWalletBackupRoute.RecoveryPhrase -> ManualBackupPhraseComponent(
            context = childContext,
            params = ManualBackupPhraseComponent.Params(
                userWalletId = model.params.userWalletId,
                callbacks = model.manualBackupPhraseModelCallbacks,
            ),
        )
        CreateWalletBackupRoute.ConfirmBackup -> ManualBackupCheckComponent(
            context = childContext,
            params = ManualBackupCheckComponent.Params(
                userWalletId = model.params.userWalletId,
                callbacks = model.manualBackupCheckModelCallbacks,
            ),
        )
        CreateWalletBackupRoute.BackupCompleted -> ManualBackupCompletedComponent(
            context = childContext,
            params = ManualBackupCompletedComponent.Params(
                userWalletId = model.params.userWalletId,
                callbacks = model.manualBackupCompletedModelCallbacks,
            ),
        )
    }
}