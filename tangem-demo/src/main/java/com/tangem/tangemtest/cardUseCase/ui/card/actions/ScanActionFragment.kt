package com.tangem.tangemtest.cardUseCase.ui.card.actions

import com.tangem.tangemtest.R
import com.tangem.tangemtest.cardUseCase.resources.ActionType
import com.tangem.tangemtest.cardUseCase.ui.card.BaseCardActionFragment

/**
[REDACTED_AUTHOR]
 */
class ScanActionFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_action_card_scan

    override fun getAction(): ActionType = ActionType.Scan
}