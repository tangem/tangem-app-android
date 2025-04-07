package com.tangem.features.onboarding.v2.twin.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.features.onboarding.v2.twin.impl.ui.TwinWalletArtworkUM

@Immutable
internal sealed class OnboardingTwinUM {

    abstract val stepIndex: Int
    abstract val isLoading: Boolean
    abstract val artwork: TwinWalletArtworkUM

    data object TopUpPrepare : OnboardingTwinUM() {
        override val stepIndex: Int = 0
        override val isLoading: Boolean = false
        override val artwork: TwinWalletArtworkUM = TwinWalletArtworkUM.Spread
    }

    data class Welcome(
        override val isLoading: Boolean = false,
        val pairCardNumber: Int = 2,
        val onContinueClick: () -> Unit = {},
    ) : OnboardingTwinUM() {
        override val stepIndex = 0
        override val artwork: TwinWalletArtworkUM = TwinWalletArtworkUM.Spread
    }

    data class ResetWarning(
        val acceptToggle: Boolean = false,
        val onAcceptClick: (Boolean) -> Unit = {},
        val onContinueClick: () -> Unit = {},
    ) : OnboardingTwinUM() {
        override val stepIndex: Int = 0
        override val isLoading: Boolean = false
        override val artwork: TwinWalletArtworkUM = TwinWalletArtworkUM.Spread
    }

    data class ScanCard(
        override val isLoading: Boolean = false,
        val artworkStep: TwinWalletArtworkUM.Leapfrog.Step = TwinWalletArtworkUM.Leapfrog.Step.FirstCard,
        val step: Step = Step.First,
        val onScanClick: () -> Unit = {},
    ) : OnboardingTwinUM() {

        enum class Step {
            First, Second, Third
        }

        val cardNumber: Int = when (artworkStep) {
            TwinWalletArtworkUM.Leapfrog.Step.FirstCard -> 1
            TwinWalletArtworkUM.Leapfrog.Step.SecondCard -> 2
        }

        override val stepIndex: Int = 1
        override val artwork: TwinWalletArtworkUM = TwinWalletArtworkUM.Leapfrog(artworkStep)
    }

    data class TopUp(
        override val isLoading: Boolean = false,
        val balance: String = "",
        val bottomSheetConfig: TangemBottomSheetConfig = TangemBottomSheetConfig.Empty,
        val onBuyCryptoClick: () -> Unit = {},
        val onShowAddressClick: () -> Unit = {},
        val onRefreshClick: () -> Unit = {},
    ) : OnboardingTwinUM() {
        override val stepIndex: Int = 2
        override val artwork: TwinWalletArtworkUM = TwinWalletArtworkUM.TopUp
    }

    fun copySealed(isLoading: Boolean = this.isLoading): OnboardingTwinUM = when (this) {
        is Welcome -> copy(isLoading = isLoading)
        is ResetWarning -> copy()
        is ScanCard -> copy(isLoading = isLoading)
        is TopUp -> copy(isLoading = isLoading)
        TopUpPrepare -> this
    }
}