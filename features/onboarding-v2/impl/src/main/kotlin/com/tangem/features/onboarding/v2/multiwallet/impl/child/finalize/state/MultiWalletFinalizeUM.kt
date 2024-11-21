package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.state

data class MultiWalletFinalizeUM(
    val step: Step = Step.Device1,
    val cardNumber: String = "",
) {
    enum class Step {
        Device1, Device2, Device3
    }
}
