package com.tangem.features.hotwallet.forgetwallet

import arrow.core.getOrElse
import com.tangem.utils.logging.TangemLogger
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.assetsdiscovery.usecase.StartAssetsDiscoveryUseCase
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.cloudbackup.usecase.DeleteCloudBackupWithRetryUseCase
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.features.hotwallet.ForgetWalletComponent
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.forgetwallet.entity.ForgetWalletUM
import com.tangem.features.hotwallet.impl.R
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ForgetWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val appScope: AppCoroutineScope,
    private val router: Router,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val uiMessageSender: UiMessageSender,
    private val startAssetsDiscoveryUseCase: StartAssetsDiscoveryUseCase,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val cloudBackupRepository: CloudBackupRepository,
    private val setCloudBackupStateUseCase: SetCloudBackupStateUseCase,
    private val deleteCloudBackupWithRetryUseCase: DeleteCloudBackupWithRetryUseCase,
) : Model() {

    private val params = paramsContainer.require<ForgetWalletComponent.Params>()

    internal val uiState: StateFlow<ForgetWalletUM>
        field = MutableStateFlow(
            ForgetWalletUM(
                onBackClick = { router.pop() },
                isCheckboxChecked = false,
                onCheckboxClick = ::onCheckboxClick,
                onForgetWalletClick = ::onForgetWalletClick,
                isForgetButtonEnabled = false,
            ),
        )

    private fun onCheckboxClick() {
        uiState.update { currentState ->
            val newValue = !currentState.isCheckboxChecked
            currentState.copy(
                isCheckboxChecked = newValue,
                isForgetButtonEnabled = newValue,
            )
        }
    }

    private fun onForgetWalletClick() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_attention),
                message = if (params.shouldDeleteCloudBackup) {
                    resourceReference(R.string.hw_remove_wallet_confirmation_with_cloud_backup)
                } else {
                    resourceReference(R.string.hw_remove_wallet_confirmation_title)
                },
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_forget),
                        isWarning = true,
                        onClick = ::forgetWallet,
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_cancel),
                        onClick = {},
                    )
                },
            ),
        )
    }

    private fun forgetWallet() {
        modelScope.launch {
            startAssetsDiscoveryUseCase.cancel(params.userWalletId)

            val hasUserWallets = deleteWalletUseCase(params.userWalletId)
                .getOrElse { error ->
                    TangemLogger.e("Unable to delete wallet: $error")

                    uiMessageSender.send(
                        message = SnackbarMessage(resourceReference(R.string.common_unknown_error)),
                    )

                    return@launch
                }

            if (params.shouldDeleteCloudBackup) {
                deleteCloudBackup()
            }

            if (hasUserWallets) {
                router.popTo(AppRoute.Details::class)
            } else {
                router.replaceAll(AppRoute.Home())
            }
        }
    }

    // appScope so the ~1 min retry window survives this screen being torn down right after forget.
    private fun deleteCloudBackup() {
        if (!hotWalletFeatureToggles.isGoogleDriveBackupEnabled) return

        val walletId = params.userWalletId.stringValue
        appScope.launch {
            cloudBackupRepository.findBackups().fold(
                ifLeft = { TangemLogger.e("Unable to find cloud backups on forget: $it") },
                ifRight = { backups ->
                    val info = backups.firstOrNull { it.walletId == walletId }
                    if (info == null) {
                        setCloudBackupStateUseCase(walletId, isBackedUp = false)
                        return@fold
                    }

                    deleteCloudBackupWithRetryUseCase(info.fileId).fold(
                        ifLeft = { TangemLogger.e("Unable to delete cloud backup on forget: $it") },
                        ifRight = { setCloudBackupStateUseCase(walletId, isBackedUp = false) },
                    )
                },
            )
        }
    }
}