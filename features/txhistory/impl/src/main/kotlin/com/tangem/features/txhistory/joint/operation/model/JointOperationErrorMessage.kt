package com.tangem.features.txhistory.joint.operation.model

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.utils.converter.Converter
import com.tangem.core.ui.R as CoreUiR

/** Dismissing the card tap is not a failure; a payload Blockaid calls out is a warning, not an app error */
internal class JointOperationErrorMessage : Converter<JointOperationSignError, DialogMessage?> {

    override fun convert(value: JointOperationSignError): DialogMessage? {
        if (value is JointOperationSignError.UserCancelled) return null

        val title = if (value is JointOperationSignError.Malicious) {
            CoreUiR.string.common_warning
        } else {
            CoreUiR.string.common_error
        }

        return DialogMessage(
            title = resourceReference(title),
            message = resourceReference(CoreUiR.string.common_something_went_wrong),
        )
    }
}