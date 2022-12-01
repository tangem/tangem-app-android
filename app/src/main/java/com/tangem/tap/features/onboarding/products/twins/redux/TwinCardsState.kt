package com.tangem.tap.features.onboarding.products.twins.redux

import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TwinCardNumber
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.features.onboarding.OnboardingWalletBalance
import com.tangem.tap.store
import org.rekotlin.StateType
import kotlin.properties.ReadOnlyProperty

/**
 * Created by Anton Zhilenkov on 21/10/2021.
 */
data class TwinCardsState(
    // if cardNumber = null -> the scanned card is not Twin
    val cardNumber: TwinCardNumber? = null,
    val mode: CreateTwinWalletMode = CreateTwinWalletMode.CreateWallet,
    val currentStep: TwinCardsStep = TwinCardsStep.Welcome,
    val userWasUnderstandIfWalletRecreate: Boolean = false,
    //  available only when creating or recreating wallets
    val twinCardsManager: TwinCardsManager? = null,

    // for the onboarding
    val pairCardId: String? = null, // available after create\recreate wallets
    val walletManager: WalletManager? = null,
    val walletBalance: OnboardingWalletBalance = OnboardingWalletBalance.loading(),
    val balanceNonCriticalError: TapError? = null,
    val balanceCriticalError: TapError? = null,
    val showConfetti: Boolean = false,
) : StateType {

    val steps: List<TwinCardsStep>
        get() = when (mode) {
            CreateTwinWalletMode.CreateWallet -> listOf(
                TwinCardsStep.None,
                TwinCardsStep.CreateFirstWallet,
                TwinCardsStep.CreateSecondWallet,
                TwinCardsStep.CreateThirdWallet,
                TwinCardsStep.TopUpWallet,
                TwinCardsStep.Done
            )
            CreateTwinWalletMode.RecreateWallet -> listOf(
                TwinCardsStep.None,
                TwinCardsStep.CreateFirstWallet,
                TwinCardsStep.CreateSecondWallet,
                TwinCardsStep.CreateThirdWallet,
                TwinCardsStep.Done
            )
        }

    val progress: Int
        get() = steps.indexOf(currentStep)

    val showAlert: Boolean
        get() = currentStep == TwinCardsStep.CreateSecondWallet || currentStep == TwinCardsStep.CreateThirdWallet

    val isBuyAllowed: Boolean by ReadOnlyProperty<Any, Boolean> { _, _ ->
        store.state.globalState.exchangeManager.availableForBuy(walletBalance.currency)
    }
}

enum class CreateTwinWalletMode { CreateWallet, RecreateWallet }

sealed class TwinCardsStep {
    object None : TwinCardsStep()
    data class WelcomeOnly(
        val scanResponse: ScanResponse,
    ) : TwinCardsStep()

    object Welcome : TwinCardsStep()
    object Warning : TwinCardsStep()
    object CreateFirstWallet : TwinCardsStep()
    object CreateSecondWallet : TwinCardsStep()
    object CreateThirdWallet : TwinCardsStep()

    // for the onboarding
    object TopUpWallet : TwinCardsStep()
    object Done : TwinCardsStep()
}
