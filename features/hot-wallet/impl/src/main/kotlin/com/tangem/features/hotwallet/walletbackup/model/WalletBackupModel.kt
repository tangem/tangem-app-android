package com.tangem.features.hotwallet.walletbackup.model

import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.components.bottomsheets.message.icon
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.secondaryButton
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.common.routing.AppRoute
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
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
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
            backedUp = false,
        ),
    )

    private val makeBackupAtFirstAlertBS
        get() = bottomSheetMessage {
            infoBlock {
                icon(R.drawable.ic_passcode_lock_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.hw_backup_need_title)
                body = resourceReference(R.string.hw_backup_need_description)
            }
            secondaryButton {
                text = resourceReference(R.string.hw_backup_need_action)
                onClick {
                    router.push(AppRoute.CreateWalletBackup(params.userWalletId))
                    closeBs()
                }
            }
        }

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
            // TODO [REDACTED_TASK_KEY]
        } else {
            uiMessageSender.send(makeBackupAtFirstAlertBS)
        }
    }
}