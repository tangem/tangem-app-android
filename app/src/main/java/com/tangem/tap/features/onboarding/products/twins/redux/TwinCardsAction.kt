package com.tangem.tap.features.onboarding.products.twins.redux

import com.tangem.Message
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.VoidCallback
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.AssetReader
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.features.onboarding.OnboardingWalletBalance
import org.rekotlin.Action

/**
 * Created by Anton Zhilenkov on 21/10/2021.
 */
sealed class TwinCardsAction : Action {
    data class IfTwinsPrepareState(val scanResponse: ScanResponse) : TwinCardsAction()

    object Init : TwinCardsAction()
    data class SetMode(val mode: CreateTwinWalletMode) : TwinCardsAction()
    data class SetStepOfScreen(val step: TwinCardsStep) : TwinCardsAction()
    data class SetUserUnderstand(val isUnderstand: Boolean) : TwinCardsAction()

    sealed class CardsManager {
        data class Set(val manager: TwinCardsManager) : TwinCardsAction()
        object Release : TwinCardsAction()
    }

    sealed class Wallet : TwinCardsAction() {
        data class HandleOnBackPressed(
            // this is necessary for the correct animation of the return of cards to the Home screen
            val shouldResetTwinCardsWidget: (should: Boolean, popAction: VoidCallback) -> Unit
        ) : TwinCardsAction()

        data class ShowInterruptDialog(val onOk: VoidCallback) : TwinCardsAction(), StateDialog

        data class LaunchFirstStep(val initialMessage: Message, val reader: AssetReader) : TwinCardsAction()
        data class LaunchSecondStep(
            val initialMessage: Message,
            val preparingMessage: Message,
            val creatingWalletMessage: Message,
        ) : TwinCardsAction()

        data class LaunchThirdStep(val message: Message) : TwinCardsAction()
    }

    // for the onboarding
    data class SetPairCardId(val cardId: String) : TwinCardsAction()
    object TopUp : TwinCardsAction()
    object ShowAddressInfoDialog : TwinCardsAction()
    data class SetWalletManager(val walletManager: WalletManager) : TwinCardsAction()
    object Done : TwinCardsAction()

    data class SaveScannedTwinCardAndNavigateToWallet(
        val scanResponse: ScanResponse,
    ) : TwinCardsAction()

    sealed class Balance {
        object Update : TwinCardsAction()
        data class Set(val balance: OnboardingWalletBalance) : TwinCardsAction()
        data class SetCriticalError(val error: TapError?) : TwinCardsAction()
        data class SetNonCriticalError(val error: TapError?) : TwinCardsAction()
    }

    sealed class Confetti {
        object Show : TwinCardsAction()
        object Hide : TwinCardsAction()
    }
}
