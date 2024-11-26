package com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
class Wallet1ChooseOptionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val userWalletsListManager: UserWalletsListManager,
    private val cardRepository: CardRepository,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private var skipClicked = false
    val returnToParentFlow = MutableSharedFlow<Unit>()

    fun onSkipClick() {
        // TODO show confirmation dialog

        if (skipClicked) return
        skipClicked = true

        modelScope.launch {
            val scanResponse = params.multiWalletState.value.currentScanResponse

            val userWallet = createUserWallet(scanResponse)
            userWalletsListManager.save(
                userWallet = userWallet,
                canOverride = true,
            )

            cardRepository.finishCardActivation(scanResponse.card.cardId)

            // save user wallet for manage tokens screen
            params.multiWalletState.update {
                it.copy(resultUserWallet = userWallet)
            }

            returnToParentFlow.emit(Unit)
        }
    }

    private suspend fun createUserWallet(scanResponse: ScanResponse): UserWallet {
        return requireNotNull(
            value = UserWalletBuilder(scanResponse, generateWalletNameUseCase).build(),
            lazyMessage = { "User wallet not created" },
        )
    }
}