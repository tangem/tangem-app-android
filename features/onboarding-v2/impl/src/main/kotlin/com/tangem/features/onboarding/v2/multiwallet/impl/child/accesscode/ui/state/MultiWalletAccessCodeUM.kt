package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.state

import androidx.compose.ui.text.input.TextFieldValue

internal data class MultiWalletAccessCodeUM(
    val step: Step = Step.Intro,
    val accessCodeFirst: TextFieldValue = TextFieldValue(""),
    val accessCodeSecond: TextFieldValue = TextFieldValue(""),
    val codesNotMatchError: Boolean = false,
    val atLeast4CharError: Boolean = false,
    val onAccessCodeFirstChange: (TextFieldValue) -> Unit = {},
    val onAccessCodeSecondChange: (TextFieldValue) -> Unit = {},
    val onContinue: () -> Unit = {},
) {
    enum class Step {
        Intro,
        AccessCode,
        ConfirmAccessCode,
    }
}