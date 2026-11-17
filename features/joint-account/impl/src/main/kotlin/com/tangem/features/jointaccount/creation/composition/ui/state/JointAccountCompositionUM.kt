package com.tangem.features.jointaccount.creation.composition.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.jointaccount.impl.R

@Immutable
internal data class JointAccountCompositionUM(
    val totalMembers: StepperUM,
    val requiredToSign: StepperUM,
    val maxMembers: Int,
    val onContinueClick: () -> Unit,
    val onBackClick: () -> Unit,
) {

    /** The account stops signing once fewer than [requiredToSign] members still have their wallets */
    val accessLossNote: TextReference
        get() {
            val total = totalMembers.value
            val membersToLoseAccess = total - requiredToSign.value + 1

            return when {
                membersToLoseAccess >= total -> resourceReference(R.string.joint_account_members_access_loss_all)
                membersToLoseAccess <= 1 -> resourceReference(R.string.joint_account_members_access_loss_any)
                else -> resourceReference(
                    id = R.string.joint_account_members_access_loss_some,
                    formatArgs = wrappedList(membersToLoseAccess.toString(), total.toString()),
                )
            }
        }

    data class StepperUM(
        val value: Int,
        val isDecrementEnabled: Boolean,
        val isIncrementEnabled: Boolean,
        val onDecrement: () -> Unit,
        val onIncrement: () -> Unit,
    )

    companion object {
        const val MIN_REQUIRED_TO_SIGN = 1
    }
}