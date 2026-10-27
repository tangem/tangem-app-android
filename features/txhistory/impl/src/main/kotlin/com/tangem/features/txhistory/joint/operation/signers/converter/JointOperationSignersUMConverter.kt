package com.tangem.features.txhistory.joint.operation.signers.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.mapFormattedDate
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

internal class JointOperationSignersUMConverter :
    Converter<JointOperationSignersUMConverter.Input, JointOperationSignersUM> {

    override fun convert(value: Input): JointOperationSignersUM {
        return JointOperationSignersUM(
            membersCount = value.membersCount,
            signers = value.signers
                .mapIndexed { index, state -> toSignerUM(index = index, state = state, isFinished = value.isFinished) }
                .toImmutableList(),
        )
    }

    private fun toSignerUM(
        index: Int,
        state: JointOperationSignerState,
        isFinished: Boolean,
    ): JointOperationSignersUM.SignerUM {
        return JointOperationSignersUM.SignerUM(
            name = state.member.name,
            avatar = avatar(member = state.member, index = index),
            statusText = statusText(state = state, isFinished = isFinished),
            mark = when (state.answer) {
                Answer.CREATED, Answer.SIGNED -> JointOperationSignersUM.Mark.Signed
                Answer.DECLINED -> JointOperationSignersUM.Mark.Declined
                Answer.WAITING -> null
            },
        )
    }

    /** The same monogram and palette rule the members screen uses, so a member looks the same in both places */
    private fun avatar(member: JointAccount.Member, index: Int): JointAccountMembersUM.MemberAvatarUM {
        val colors = CryptoPortfolioIcon.Color.entries

        return JointAccountMembersUM.MemberAvatarUM(
            monogram = member.name.take(n = 1).uppercase(),
            color = colors[index % colors.size],
        )
    }

    /**
     * A member the backend reported without a time is shown answered but undated, never with a blank prefix. A member
     * who never answered a finished operation is no longer waiting for anything.
     */
    private fun statusText(state: JointOperationSignerState, isFinished: Boolean): TextReference {
        val at = state.at

        return when {
            state.answer == Answer.WAITING && isFinished -> TextReference.EMPTY
            state.answer == Answer.WAITING -> resourceReference(CoreUiR.string.joint_operation_waiting_to_sign)
            state.answer == Answer.DECLINED && at == null -> resourceReference(CoreUiR.string.joint_operation_declined)
            at == null -> TextReference.EMPTY
            else -> resourceReference(
                id = when (state.answer) {
                    Answer.CREATED -> CoreUiR.string.joint_operation_created_at
                    Answer.DECLINED -> CoreUiR.string.joint_operation_declined_at
                    else -> CoreUiR.string.joint_operation_signed_at
                },
                formatArgs = wrappedList(mapFormattedDate(createdAt = at)),
            )
        }
    }

    data class Input(
        val membersCount: Int,
        val signers: List<JointOperationSignerState>,
        val isFinished: Boolean = false,
    )
}