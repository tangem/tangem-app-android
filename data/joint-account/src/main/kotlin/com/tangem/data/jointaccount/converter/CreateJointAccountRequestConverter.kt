package com.tangem.data.jointaccount.converter

import com.tangem.datasource.api.jointaccount.models.CreateJointAccountRequest
import com.tangem.datasource.api.jointaccount.models.JointAccountConfigDto
import com.tangem.datasource.api.jointaccount.models.JointAccountParticipantDto
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload

/**
 * Domain payload → creation request DTO.
 *
 * Values are copied verbatim: the signature is verified against the canonical form of exactly these values,
 * so trimming or normalizing any string here would invalidate it.
 */
internal object CreateJointAccountRequestConverter {

    fun convert(payload: JointAccountCreationPayload, signature: String): CreateJointAccountRequest {
        return CreateJointAccountRequest(
            payload = CreateJointAccountRequest.Payload(
                config = JointAccountConfigDto(
                    name = payload.config.name,
                    icon = payload.config.icon,
                    iconColor = payload.config.iconColor,
                    membersCount = payload.config.membersCount,
                    threshold = payload.config.threshold,
                ),
                creator = JointAccountParticipantDto(
                    walletId = payload.creator.walletId,
                    name = payload.creator.name,
                    address = payload.creator.address,
                    derivation = payload.creator.derivation,
                ),
            ),
            signature = signature,
        )
    }
}