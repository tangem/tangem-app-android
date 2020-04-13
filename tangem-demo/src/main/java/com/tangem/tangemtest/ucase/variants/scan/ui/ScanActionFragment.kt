package com.tangem.tangemtest.ucase.variants.scan.ui

import com.tangem.commands.Card
import com.tangem.tangemtest.R
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.ScanItemsManager
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment

/**
[REDACTED_AUTHOR]
 */
class ScanActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { ScanItemsManager() }

    override fun getLayoutId(): Int = R.layout.fg_action_card_scan

    override fun initFab() {
        actionFab.setOnClickListener { actionVM.invokeMainAction() }
    }

    override fun responseCardDataHandled(card: Card?) {
        super.responseCardDataHandled(card)
        navigateTo(R.id.action_nav_card_action_to_response_screen)
    }
}