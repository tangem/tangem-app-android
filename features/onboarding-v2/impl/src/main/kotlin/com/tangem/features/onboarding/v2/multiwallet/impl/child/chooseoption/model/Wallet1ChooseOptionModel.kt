package com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.common.TapWorkarounds.canSkipBackup
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.onboarding.v2.multiwallet.impl.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class Wallet1ChooseOptionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val cardRepository: CardRepository,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val analyticsHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private var skipClicked = false
    val returnToParentFlow = MutableSharedFlow<Unit>()

    init {
        analyticsHandler.send(OnboardingEvent.Backup.ScreenOpened)
    }

    val canSkipBackup = params.multiWalletState.value.currentScanResponse.card.canSkipBackup

    fun onSkipClick() {
        if (skipClicked) return
        skipClicked = true

        analyticsHandler.send(OnboardingEvent.Backup.Skipped)

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

                    returnToParentFlow.emit(Unit)
                }
        }
    }

    private suspend fun createUserWallet(scanResponse: ScanResponse): UserWallet {
        return requireNotNull(
            value = UserWalletBuilder(scanResponse, generateWalletNameUseCase).build(),
            lazyMessage = { "User wallet not created" },
        )
    }
}