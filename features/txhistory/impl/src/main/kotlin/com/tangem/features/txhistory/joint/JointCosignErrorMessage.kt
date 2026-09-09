package com.tangem.features.txhistory.joint

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.utils.converter.Converter
import com.tangem.core.ui.R as CoreUiR

/**
 * What the user sees when signing an operation fails. Dismissing the card tap is not a failure and produces no
 * message; a payload Blockaid calls out is a warning rather than an app error.
 */
internal class JointCosignErrorMessage : Converter<JointOperationSignError, DialogMessage?> {

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