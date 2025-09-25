package com.tangem.features.onboarding.v2.multiwallet.impl.child.upgradewallet.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.upgradewallet.ui.state.MultiWalletUpgradeWalletUM
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.resetCardDialog
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState.Step
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class MultiWalletUpgradeWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val cardRepository: CardRepository,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val multiWalletState
        get() = params.multiWalletState

    private val _uiState = MutableStateFlow(
        MultiWalletUpgradeWalletUM(
            title = resourceReference(R.string.hw_upgrade_start_title),
            bodyText = resourceReference(R.string.hw_upgrade_start_description),
            onStartUpgradeClick = {
                // TODO [REDACTED_TASK_KEY] track button click
                upgradeWallet(false)
            },
            dialog = null,
        ),
    )

    val uiState: StateFlow<MultiWalletUpgradeWalletUM> = _uiState
    val onDone = MutableSharedFlow<Step>()

    init {
        // TODO [REDACTED_TASK_KEY] track screen opened
    }

    private fun upgradeWallet(shouldReset: Boolean) {
        modelScope.launch {
            val mode = params.parentParams.mode
            require(mode is OnboardingMultiWalletComponent.Mode.UpgradeHotWallet)

            val userWallet = getUserWalletUseCase(mode.userWalletId)
                .getOrElse { error("User wallet with id ${mode.userWalletId} not found") }
            if (userWallet is UserWallet.Hot) {
                val privateInfo = exportSeedPhraseUseCase
                    .invoke(userWallet.hotWalletId)
                    .getOrElse { error("Unable to export seed phrase for wallet with id ${mode.userWalletId}") }

                modelScope.launch {
                    val result = tangemSdkManager.importWallet(
                        scanResponse = multiWalletState.value.currentScanResponse,
                        shouldReset = shouldReset,
                        mnemonic = privateInfo.mnemonic.mnemonicComponents.joinToString(" "),
                        passphrase = privateInfo.passphrase?.concatToString(),
                    )

                    when (result) {
                        is CompletionResult.Success -> {
                            multiWalletState.update {
                                it.copy(
                                    currentScanResponse = it.currentScanResponse.copy(
                                        card = result.data.card,
                                        derivedKeys = result.data.derivedKeys,
                                        primaryCard = result.data.primaryCard,
                                    ),
                                )
                            }

                            cardRepository.startCardActivation(cardId = result.data.card.cardId)

                            // TODO [REDACTED_TASK_KEY] track wallet created
                            val cardDoesNotSupportBackup = result.data.card.settings.isBackupAllowed.not()
                            when {
                                cardDoesNotSupportBackup -> createWalletAndNavigateBackWithDone()
                                params.parentParams.withSeedPhraseFlow -> onDone.emit(Step.AddBackupDevice)
                                else -> {
                                    onDone.emit(Step.ChooseBackupOption)
                                }
                            }
                        }

                        is CompletionResult.Failure -> {
                            if (result.error is TangemSdkError.WalletAlreadyCreated) {
                                // show should reset dialog
                                handleActivationError()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createWalletAndNavigateBackWithDone() {
        modelScope.launch {
            val scanResponse = params.multiWalletState.value.currentScanResponse

            val userWallet = createUserWallet(scanResponse)
            saveWalletUseCase(userWallet, canOverride = true)
                .onRight {
                    cardRepository.finishCardActivation(scanResponse.card.cardId)

                    // save user wallet for manage tokens screen
                    params.multiWalletState.update {
                        it.copy(resultUserWallet = userWallet)
                    }

                    onDone.emit(Step.Done)
                }
        }
    }

    private fun createUserWallet(scanResponse: ScanResponse): UserWallet.Cold {
        return requireNotNull(
            value = coldUserWalletBuilderFactory.create(scanResponse = scanResponse).build(),
            lazyMessage = { "User wallet not created" },
        )
    }

    private fun handleActivationError() {
        _uiState.update { state ->
            state.copy(
                dialog = resetCardDialog(
                    onConfirm = ::navigateToSupportScreen,
                    dismiss = { _uiState.update { it.copy(dialog = null) } },
                    onDismissButtonClick = ::resetCard,
                ),
            )
        }
    }

    private fun resetCard() {
        upgradeWallet(true)
    }

    fun navigateToSupportScreen() {
        modelScope.launch {
            val cardInfo =
                getWalletMetaInfoUseCase(multiWalletState.value.currentScanResponse).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(cardInfo))
        }
    }
}