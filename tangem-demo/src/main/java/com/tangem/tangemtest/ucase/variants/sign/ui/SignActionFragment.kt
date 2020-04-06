package com.tangem.tangemtest.ucase.variants.sign.ui

import com.tangem.tangemtest.R
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment

/**
[REDACTED_AUTHOR]
 */
class SignActionFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_action_card_sign

    override fun getAction(): ActionType = ActionType.Sign
}