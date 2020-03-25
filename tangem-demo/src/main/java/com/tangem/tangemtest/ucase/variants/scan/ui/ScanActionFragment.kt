package com.tangem.tangemtest.ucase.variants.scan.ui

import com.tangem.tangemtest.R
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment

/**
[REDACTED_AUTHOR]
 */
class ScanActionFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_action_card_scan

    override fun getAction(): ActionType = ActionType.Scan
}