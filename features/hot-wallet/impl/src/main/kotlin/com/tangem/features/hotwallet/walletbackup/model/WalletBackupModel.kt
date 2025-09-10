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
import com.tangem.common.routing.AppRoute
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.WalletBackupComponent
import com.tangem.features.hotwallet.walletbackup.entity.BackupStatus
import com.tangem.features.hotwallet.walletbackup.entity.WalletBackupUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
internal class WalletBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val getWalletUseCase: GetUserWalletUseCase,
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    private val params: WalletBackupComponent.Params = paramsContainer.require()

    val uiState: StateFlow<WalletBackupUM>
    field = MutableStateFlow(
        WalletBackupUM(
            onBackClick = { router.pop() },
            recoveryPhraseOption = LabelUM(
                text = resourceReference(R.string.hw_backup_no_backup),
                style = LabelStyle.WARNING,
            ),
            googleDriveOption = LabelUM(
                text = resourceReference(R.string.common_coming_soon),
                style = LabelStyle.REGULAR,
            ),
            googleDriveStatus = BackupStatus.ComingSoon,
            onRecoveryPhraseClick = ::onRecoveryPhraseClick,
            onGoogleDriveClick = { },
            onHardwareWalletClick = ::onHardwareWalletClick,
            backedUp = false,
        ),
    )

    init {
        getWalletUseCase.invoke(params.userWalletId)
            .fold(
                ifLeft = {
                    Timber.e("Error on getting user wallet: $it")
                },
                ifRight = {
                    updateBackupStatuses(it)
                },
            )
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
        recoveryPhraseOption = if (userWallet.backedUp) {
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
        googleDriveOption = LabelUM(
            text = resourceReference(R.string.common_coming_soon),
            style = LabelStyle.REGULAR,
        ),
        backedUp = userWallet.backedUp,
    )

    private fun onRecoveryPhraseClick() {
        if (uiState.value.backedUp) {
            getWalletUseCase.invoke(params.userWalletId)
                .fold(
                    ifLeft = {
                        Timber.e("Error on getting user wallet: $it")
                    },
                    ifRight = { userWallet ->
                        when (userWallet) {
                            is UserWallet.Cold -> {
                                val userWalletId = userWallet.walletId
                                Timber.e("Unexpected cold wallet when request seed phrase: $userWalletId")
                            }
                            is UserWallet.Hot -> showSeedPhrase(userWallet)
                        }
                    },
                )
        } else {
            router.push(AppRoute.CreateWalletBackup(params.userWalletId))
        }
    }

    private fun showSeedPhrase(hotWallet: UserWallet.Hot) {
        modelScope.launch {
            unlockHotWalletContextualUseCase.invoke(hotWallet.hotWalletId)
                .fold(
                    ifLeft = {
                        Timber.e("Error while export seed phrase: $it")
                    },
                    ifRight = { seedPhrasePrivateInfo ->
                        router.push(AppRoute.ViewPhrase(params.userWalletId))
                    },
                )
        }
    }

    private fun onHardwareWalletClick() {
        router.push(AppRoute.UpgradeWallet(params.userWalletId))
    }
}