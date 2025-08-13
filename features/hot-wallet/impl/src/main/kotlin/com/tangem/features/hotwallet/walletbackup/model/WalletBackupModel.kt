package com.tangem.features.hotwallet.walletbackup.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.hotwallet.WalletBackupComponent
import com.tangem.features.hotwallet.walletbackup.entity.BackupStatus
import com.tangem.features.hotwallet.walletbackup.entity.WalletBackupUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class WalletBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getWalletUseCase: GetUserWalletUseCase,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: WalletBackupComponent.Params = paramsContainer.require()

    val uiState: StateFlow<WalletBackupUM>
    field = MutableStateFlow(
        WalletBackupUM(
            onBackClick = { router.pop() },
            recoveryPhraseStatus = BackupStatus.NoBackup,
            googleDriveStatus = BackupStatus.ComingSoon,
            onRecoveryPhraseClick = { },
            onGoogleDriveClick = { },
        ),
    )

    init {
        getWalletUseCase.invokeFlow(params.userWalletId)
            .map { it.getOrNull() }
            .distinctUntilChanged()
            .onEach {
                updateBackupStatuses()
            }
            .launchIn(modelScope)
    }

    private fun updateBackupStatuses() {
        uiState.update { currentState ->
            currentState.copy(
                recoveryPhraseStatus = BackupStatus.NoBackup,
                googleDriveStatus = BackupStatus.ComingSoon,
            )
        }
    }
}