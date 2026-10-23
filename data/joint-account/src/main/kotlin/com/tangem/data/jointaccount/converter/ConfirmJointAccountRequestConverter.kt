package com.tangem.data.jointaccount.converter

import com.tangem.domain.jointaccount.model.JointAccountConfirmationPayload
import com.tangem.store.datasource.jointaccount.models.ConfirmJointAccountRequest
import com.tangem.utils.converter.Converter

internal class ConfirmJointAccountRequestConverter(
    private val signature: String,
) : Converter<JointAccountConfirmationPayload, ConfirmJointAccountRequest> {

    override fun convert(value: JointAccountConfirmationPayload): ConfirmJointAccountRequest {
        return ConfirmJointAccountRequest(
            payload = ConfirmJointAccountRequest.Payload(
                name = value.config.name,
                icon = value.config.icon,
                iconColor = value.config.iconColor,
                membersCount = value.config.membersCount,
                threshold = value.config.threshold,
                safeAddress = value.safeAddress,
            ),
            signature = signature,
        )
    }
}