package com.tangem.features.onboarding.v2.visa.impl.child.accesscode.ui.state

import androidx.compose.ui.text.input.TextFieldValue

internal data class OnboardingVisaAccessCodeUM(
    val step: Step = Step.Enter,
    val accessCodeFirst: TextFieldValue = TextFieldValue(""),
    val accessCodeSecond: TextFieldValue = TextFieldValue(""),
    val hasCodesNotMatchError: Boolean = false,
    val hasAtLeastMinCharsError: Boolean = false,
    val isAccessCodeHidden: Boolean = true,
    val isButtonLoading: Boolean = false,
    val onAccessCodeFirstChange: (TextFieldValue) -> Unit = {},
    val onAccessCodeSecondChange: (TextFieldValue) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onAccessCodeHideClick: () -> Unit = {},
) {
    enum class Step {
        Enter, ReEnter
    }
}