package com.tangem.features.askbiometry.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.settings.SetSaveWalletScreenShownUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.features.askbiometry.AskBiometryComponent
import com.tangem.features.askbiometry.impl.ui.state.AskBiometryUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class AskBiometryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val setSaveWalletScreenShownUseCase: SetSaveWalletScreenShownUseCase,
    private val settingsRepository: SettingsRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val userWalletsListManager: UserWalletsListManager,
    private val walletsRepository: WalletsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val settingsManager: SettingsManager,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<AskBiometryComponent.Params>()

    private val _uiState = MutableStateFlow(
        AskBiometryUM(
            bottomSheetVariant = params.bottomSheetVariant,
            onAllowClick = ::onAllowClick,
            onDontAllowClick = ::dontAllow,
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        modelScope.launch {
            setSaveWalletScreenShownUseCase()
        }
    }

    private fun onAllowClick() {
        _uiState.update { it.copy(showProgress = true) }
        allowToUseBiometrics()
    }

    private fun dontAllow() {
        params.modelCallbacks.onDenied()
    }

    fun dismiss() {
        dontAllow()
    }

    private fun allowToUseBiometrics() {
        modelScope.launch {
            if (tangemSdkManager.checkNeedEnrollBiometrics()) {
                showEnrollBiometricsDialog()
                return@launch
            }

            /*
             * We don't need to save user wallet if it is not created from backup info,
             * because it will be automatically saved on UserWalletsListManager switch
             */
            val selectedUserWallet = userWalletsListManager.selectedUserWalletSync ?: run {
                Timber.e("Unable to save user wallet")
                uiMessageSender.send(
                    SnackbarMessage(stringReference("No selected user wallet")),
                )

                return@launch
            }

            handleSuccessAllowing(selectedUserWallet)
        }
    }

    private suspend fun handleSuccessAllowing(userWallet: UserWallet) {
        walletsRepository.saveShouldSaveUserWallets(item = true)
        settingsRepository.setShouldSaveAccessCodes(value = true)

        cardSdkConfigRepository.setAccessCodeRequestPolicy(
            isBiometricsRequestPolicy = userWallet.hasAccessCode,
        )

        params.modelCallbacks.onAllowed()
    }

    private fun showEnrollBiometricsDialog() {
        uiMessageSender.send(
            DialogMessage(
                message = resourceReference(R.string.save_user_wallet_agreement_enroll_biometrics_description),
                title = resourceReference(R.string.save_user_wallet_agreement_enroll_biometrics_title),
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.common_enable),
                    onClick = { settingsManager.openBiometricSettings() },
                ),
                secondAction = EventMessageAction(
                    title = resourceReference(R.string.common_cancel),
                    onClick = {},
                ),
                dismissOnFirstAction = true,
            ),
        )
    }
}
