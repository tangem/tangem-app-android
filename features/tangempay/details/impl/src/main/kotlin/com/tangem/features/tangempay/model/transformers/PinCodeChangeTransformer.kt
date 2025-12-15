package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayChangePinUM
import com.tangem.features.tangempay.utils.PinCodeValidation
import com.tangem.utils.transformer.Transformer

internal class PinCodeChangeTransformer(
    private val newPin: String,
) : Transformer<TangemPayChangePinUM> {

    override fun transform(prevState: TangemPayChangePinUM): TangemPayChangePinUM {
        val valid = PinCodeValidation.validate(pinCode = newPin)
        // Do not show error yet if user didn't fill all 4 spaces
        val isError = PinCodeValidation.validateLength(pinCode = newPin) && !valid
        return prevState.copy(
            pinCode = newPin,
            submitButtonEnabled = valid,
            error = if (isError) {
                resourceReference(R.string.visa_onboarding_pin_validation_error_message)
            } else {
                null
            },
        )
    }
}