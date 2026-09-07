package com.tangem.data.jointaccount.converter

import com.tangem.domain.jointaccount.model.JointAccountJoinPayload
import com.tangem.store.datasource.jointaccount.models.JoinJointAccountRequest
import com.tangem.store.datasource.jointaccount.models.JointAccountConfigDto
import com.tangem.store.datasource.jointaccount.models.JointAccountParticipantDto
import com.tangem.utils.converter.Converter

internal class JoinJointAccountRequestConverter(
    private val signature: String,
) : Converter<JointAccountJoinPayload, JoinJointAccountRequest> {

    override fun convert(value: JointAccountJoinPayload): JoinJointAccountRequest {
        return JoinJointAccountRequest(
            payload = JoinJointAccountRequest.Payload(
                inviteId = value.inviteId,
                config = JointAccountConfigDto(
                    name = value.config.name,
                    icon = value.config.icon,
                    iconColor = value.config.iconColor,
                    membersCount = value.config.membersCount,
                    threshold = value.config.threshold,
                ),
                member = JointAccountParticipantDto(
                    walletId = value.member.walletId,
                    name = value.member.name,
                    address = value.member.address,
                    derivation = value.member.derivation,
                ),
            ),
            signature = signature,
        )
    }
}