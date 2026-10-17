package com.tangem.features.moneyaccount.onboarding.ui

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class MoneyAccountOnboardingUM {

    abstract val onBack: () -> Unit

    data class Loading(override val onBack: () -> Unit) : MoneyAccountOnboardingUM()

    data class Content(
        override val onBack: () -> Unit,
        val isLoading: Boolean,
        val fees: FeesUM?,
        val onGetStartedClick: () -> Unit,
        val onTermsAndFeesClick: () -> Unit,
        val onTermsClick: () -> Unit,
        val onFeesDismiss: () -> Unit,
    ) : MoneyAccountOnboardingUM()

    @Immutable
    sealed interface FeesUM {

        data object Loading : FeesUM

        data class Content(val rows: ImmutableList<FeeRowUM>) : FeesUM

        data class Error(val onRetryClick: () -> Unit) : FeesUM
    }

    @Immutable
    data class FeeRowUM(
        val icon: TangemIconUM,
        val provider: String,
        val receive: String,
        val send: String,
    )
}