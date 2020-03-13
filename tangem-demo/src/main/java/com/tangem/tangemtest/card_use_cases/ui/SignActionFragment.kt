package com.tangem.tangemtest.card_use_cases.ui

import com.tangem.tangemtest.R
import com.tangem.tangemtest.commons.Action

/**
[REDACTED_AUTHOR]
 */
class SignActionFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_action_card_sign

    override fun getAction(): Action = Action.Sign
}