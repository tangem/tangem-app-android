package com.tangem.tap.features.twins.redux

import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.twins.TwinCardNumber
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class TwinCardsState(
    // if cardNumber is not set -> the scanned card is not Twin
    val cardNumber: TwinCardNumber? = null,
    val secondCardId: String? = null,
    val showTwinOnboarding: Boolean = false,
    val isCreatingTwinCardsAllowed: Boolean = false,
    val createWalletState: CreateTwinWalletState? = null
) : StateType {
    val isReadyToUse: Boolean
        get() = cardNumber != null
}

data class CreateTwinWalletState(
    val scanResponse: ScanResponse?,
    val number: TwinCardNumber = TwinCardNumber.First,
    val mode: CreateTwinWalletMode = CreateTwinWalletMode.CreateWallet,
    val step: CreateTwinWalletStep = CreateTwinWalletStep.FirstStep,
) {
    val showAlert: Boolean
        get() = mode != CreateTwinWalletMode.RecreateWallet
}

enum class CreateTwinWalletStep { FirstStep, SecondStep, ThirdStep }

enum class CreateTwinWalletMode { CreateWallet, RecreateWallet }
