package com.tangem.data.jointaccount.converter

import com.tangem.domain.jointaccount.model.JointAccountActivationPayload
import com.tangem.store.datasource.jointaccount.models.ActivateJointAccountRequest
import com.tangem.utils.converter.Converter

internal class ActivateJointAccountRequestConverter(
    private val signature: String,
) : Converter<JointAccountActivationPayload, ActivateJointAccountRequest> {

    override fun convert(value: JointAccountActivationPayload): ActivateJointAccountRequest {
        return ActivateJointAccountRequest(
            payload = ActivateJointAccountRequest.Payload(
                walletId = value.walletId,
                cryptoAccountId = value.cryptoAccountId,
                config = ActivateJointAccountRequest.ConfirmedConfig(
                    name = value.config.base.name,
                    icon = value.config.base.icon,
                    iconColor = value.config.base.iconColor,
                    membersCount = value.config.base.membersCount,
                    threshold = value.config.base.threshold,
                    safeAddress = value.config.safeAddress,
                ),
            ),
            signature = signature,
        )
    }
}