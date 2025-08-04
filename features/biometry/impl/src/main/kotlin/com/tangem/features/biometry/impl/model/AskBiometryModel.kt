package com.tangem.features.biometry.impl.model

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
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.SetSaveWalletScreenShownUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.biometry.impl.ui.state.AskBiometryUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
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
            onDismiss = ::dismiss,
        ),
    )
    val uiState = _uiState.asStateFlow()
    val dismissBSFlow = MutableSharedFlow<Unit>()

    init {
        modelScope.launch {
            setSaveWalletScreenShownUseCase()
        }
    }

    fun dismiss() {
        modelScope.launch {
            dismissBSFlow.emit(Unit)
            delay(timeMillis = 500)
            dontAllow()
        }
    }

    private fun onAllowClick() {
        modelScope.launch {
            if (tangemSdkManager.checkNeedEnrollBiometrics()) {
                showEnrollBiometricsDialog()
                return@launch
            }

            _uiState.update { it.copy(showProgress = true) }

            /*

             * because it will be automatically saved on UserWalletsListManager switch
             */
            val selectedUserWallet = userWalletsListManager.selectedUserWalletSync ?: run {
                Timber.e("Unable to save user wallet")
                uiMessageSender.send(
                    SnackbarMessage(stringReference("No selected user wallet")),
                )
                _uiState.update { it.copy(showProgress = false) }

                return@launch
            }

            handleSuccessAllowing(selectedUserWallet)
        }
    }

    private fun dontAllow() {
        params.modelCallbacks.onDenied()
    }

    private suspend fun handleSuccessAllowing(userWallet: UserWallet) {
        walletsRepository.saveShouldSaveUserWallets(item = true)
        settingsRepository.setShouldSaveAccessCodes(value = true)

        if (userWallet is UserWallet.Cold) {
            cardSdkConfigRepository.setAccessCodeRequestPolicy(
                isBiometricsRequestPolicy = userWallet.hasAccessCode,
            )
        }

        if (_uiState.value.bottomSheetVariant) {
            dismissBSFlow.emit(Unit)
            delay(timeMillis = 500)
        }

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