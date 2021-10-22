package com.tangem.tap.features.twins.redux

import com.tangem.Message
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.twins.AssetReader
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.domain.twins.TwinCardsManager
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class TwinCardsAction : Action {

    data class SetResources(val resources: TwinCardsResources) : TwinCardsAction()

    data class SetTwinCard(
        val number: TwinCardNumber,
        val secondCardId: String?,
        val isCreatingTwinCardsAllowed: Boolean,
    ) : TwinCardsAction()

    sealed class CardsManager {
        data class Set(val manager: TwinCardsManager) : TwinCardsAction()
        object Release : TwinCardsAction()
    }

    object ShowOnboarding : TwinCardsAction()
    object SetOnboardingShown : TwinCardsAction()
    object ProceedToCreateWallet : TwinCardsAction()

    sealed class Wallet : TwinCardsAction() {
        object HandleOnBackPressed : TwinCardsAction()

        data class Create(
            val number: TwinCardNumber,
            val createTwinWalletMode: CreateTwinWalletMode
        ) : TwinCardsAction()

        data class InterruptDialog(val interrupt: () -> Unit) : TwinCardsAction(), StateDialog

        data class LaunchFirstStep(
            val initialMessage: Message,
            val reader: AssetReader
        ) : TwinCardsAction() {
            object Success : TwinCardsAction()
        }

        data class LaunchSecondStep(
            val initialMessage: Message,
            val preparingMessage: Message,
            val creatingWalletMessage: Message,
        ) : TwinCardsAction() {
            object Success : TwinCardsAction()
        }

        data class LaunchThirdStep(val message: Message) : TwinCardsAction() {
            data class Success(val scanResponse: ScanResponse) : TwinCardsAction()
        }
    }
}