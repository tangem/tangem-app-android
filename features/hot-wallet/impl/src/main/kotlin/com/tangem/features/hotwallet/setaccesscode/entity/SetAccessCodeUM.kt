package com.tangem.features.hotwallet.setaccesscode.entity

internal data class SetAccessCodeUM(
    val step: Step,
    val accessCodeFirst: String,
    val accessCodeSecond: String,
    val onAccessCodeFirstChange: (String) -> Unit,
    val onAccessCodeSecondChange: (String) -> Unit,
    val buttonEnabled: Boolean,
    val onContinue: () -> Unit,
) {
    val accessCodeLength: Int = ACCESS_CODE_LENGTH

    enum class Step {
        AccessCode,
        ConfirmAccessCode,
    }

    companion object {
        private const val ACCESS_CODE_LENGTH = 6
    }
}