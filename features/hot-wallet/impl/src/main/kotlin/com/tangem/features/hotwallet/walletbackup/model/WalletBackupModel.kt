package com.tangem.features.hotwallet.walletbackup.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.R
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.hotwallet.WalletBackupComponent
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
            recoveryPhraseStatus = LabelUM(
                text = resourceReference(R.string.hw_backup_no_backup),
                style = LabelStyle.WARNING,
            ),
            googleDriveStatus = LabelUM(
                text = resourceReference(R.string.common_coming_soon),
                style = LabelStyle.REGULAR,
            ),
            onRecoveryPhraseClick = { },
            onGoogleDriveClick = { },
        ),
    )

    init {
        getWalletUseCase.invokeFlow(params.userWalletId)
            .map { it.getOrNull() }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach {
                updateBackupStatuses(it)
            }
            .launchIn(modelScope)
    }

    private fun updateBackupStatuses(userWallet: UserWallet) {
        uiState.update { currentState ->
            if (userWallet is UserWallet.Hot) {
                currentState.updateBackupStatusesHotWallet(userWallet)
            } else {
                currentState
            }
        }
    }

    private fun WalletBackupUM.updateBackupStatusesHotWallet(userWallet: UserWallet.Hot): WalletBackupUM = copy(
        recoveryPhraseStatus = if (userWallet.backedUp) {
            LabelUM(
                text = resourceReference(R.string.common_done),
                style = LabelStyle.ACCENT,
            )
        } else {
            LabelUM(
                text = resourceReference(R.string.hw_backup_no_backup),
                style = LabelStyle.WARNING,
            )
        },
        googleDriveStatus = LabelUM(
            text = resourceReference(R.string.common_coming_soon),
            style = LabelStyle.REGULAR,
        ),
    )
}