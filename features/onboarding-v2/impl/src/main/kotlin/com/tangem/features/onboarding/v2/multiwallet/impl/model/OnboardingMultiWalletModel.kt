package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.scan.CardDTO
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class OnboardingMultiWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<OnboardingMultiWalletComponent.Params>()

    val state = MutableStateFlow(
        OnboardingMultiWalletState(
            currentStep = getInitialStep(),
            currentScanResponse = params.scanResponse,
        ),
    )

    init {
        initScreenTitleSub()
    }

    private fun getInitialStep(): OnboardingMultiWalletState.Step {
        val card = params.scanResponse.card

        return when {
            card.wallets.isNotEmpty() && card.backupStatus == CardDTO.BackupStatus.NoBackup ->
                OnboardingMultiWalletState.Step.AddBackupDevice
            card.wallets.isNotEmpty() && card.backupStatus?.isActive == true ->
                OnboardingMultiWalletState.Step.FinishBackup
            else ->
                OnboardingMultiWalletState.Step.CreateWallet
        }
    }

    private fun initScreenTitleSub() {
        val title = screenTitleByStep(getInitialStep())
        params.titleProvider.changeTitle(title)

        modelScope.launch {
            state
                .map { it.currentStep }
                .collectLatest { step ->
                    val title = screenTitleByStep(step)
                    params.titleProvider.changeTitle(title)
                }
        }
    }
}
