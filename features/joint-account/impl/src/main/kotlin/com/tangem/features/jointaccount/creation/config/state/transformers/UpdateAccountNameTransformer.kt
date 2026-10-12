package com.tangem.features.jointaccount.creation.config.state.transformers

import com.tangem.domain.models.account.AccountName
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.utils.transformer.Transformer

/**
 * Applies the user's name input.
 *
 * Input past [AccountName.MAX_LENGTH] is rejected as a whole: typing the 21st character and pasting an over-long
 * value both keep the previous state. The field keeps the input as typed, no truncation; only the edge whitespace
 * is dropped later, when the signed config is built, so that it equals the name the account record will hold.
 */
internal class UpdateAccountNameTransformer(
    private val name: String,
    private val isValuePasted: Boolean = false,
) : Transformer<JointAccountConfigUM> {

    override fun transform(prevState: JointAccountConfigUM): JointAccountConfigUM {
        if (name.length > AccountName.MAX_LENGTH) return prevState

        return prevState.copy(name = name, isContinueEnabled = isNameValid(name), isValuePasted = isValuePasted)
    }

    /** [AccountName.Custom] is a boolean gate only: its value does not replace the field's content */
    private fun isNameValid(name: String): Boolean = AccountName.Custom(value = name).isRight()
}