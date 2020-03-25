package com.tangem.tangemtest.cardUseCase.ui.card.actions

import com.tangem.tangemtest.R
import com.tangem.tangemtest.cardUseCase.resources.ActionType
import com.tangem.tangemtest.cardUseCase.ui.card.BaseCardActionFragment

/**
[REDACTED_AUTHOR]
 */
class SignActionFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_action_card_sign

    override fun getAction(): ActionType = ActionType.Sign
}