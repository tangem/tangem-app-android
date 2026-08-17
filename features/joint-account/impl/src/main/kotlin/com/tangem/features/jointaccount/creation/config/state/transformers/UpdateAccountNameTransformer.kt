package com.tangem.features.jointaccount.creation.config.state.transformers

import com.tangem.domain.models.account.AccountName
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.utils.transformer.Transformer

/**
 * Applies the user's name input.
 *
 * Input past [AccountName.MAX_LENGTH] is rejected as a whole: typing the 21st character and pasting an over-long
 * value both keep the previous state. Rejecting instead of trimming is deliberate — the name goes into the signed
 * config exactly as typed, so the client must not normalize it (no trim, no truncation).
 */
internal class UpdateAccountNameTransformer(
    private val name: String,
) : Transformer<JointAccountConfigUM> {

    override fun transform(prevState: JointAccountConfigUM): JointAccountConfigUM {
        if (name.length > AccountName.MAX_LENGTH) return prevState

        return prevState.copy(name = name, isContinueEnabled = isNameValid(name))
    }

    /**
     * [AccountName.Custom] is a boolean gate only: its trimmed value must never end up in the state or the payload.
     */
    private fun isNameValid(name: String): Boolean = AccountName.Custom(value = name).isRight()
}