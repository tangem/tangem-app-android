package com.tangem.tap.features.twins.redux

import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.domain.twins.TwinCardsManager
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class TwinCardsState(
    val twinCardsManager: TwinCardsManager? = null,
    // if cardNumber is not set -> the scanned card is not Twin
    val cardNumber: TwinCardNumber? = null,
    val secondCardId: String? = null,
    val showTwinOnboarding: Boolean = false,
    val isCreatingTwinCardsAllowed: Boolean = false,
    val createWalletState: CreateTwinWalletState? = null,
    val resources: TwinCardsResources = TwinCardsResources(),
) : StateType

data class CreateTwinWalletState(
    val scanResponse: ScanResponse?,
    val number: TwinCardNumber = TwinCardNumber.First,
    val mode: CreateTwinWalletMode = CreateTwinWalletMode.CreateWallet,
    val step: CreateTwinWalletStep = CreateTwinWalletStep.FirstStep,
) {
    val showAlert: Boolean
        get() = step != CreateTwinWalletStep.FirstStep
}

enum class CreateTwinWalletStep { FirstStep, SecondStep, ThirdStep }

enum class CreateTwinWalletMode { CreateWallet, RecreateWallet }

data class TwinCardsResources(
    val strings: Strings = Strings()
) {
    //TODO: create global structure for StringRes. Set it from the MainActivity.
    data class Strings(
        val walletIsNotEmpty: Int = -1
    )
}
