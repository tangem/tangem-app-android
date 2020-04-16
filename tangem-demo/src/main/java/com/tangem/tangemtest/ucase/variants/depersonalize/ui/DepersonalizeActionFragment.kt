package com.tangem.tangemtest.ucase.variants.depersonalize.ui

import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.DepersonalizeItemsManager
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment

/**
[REDACTED_AUTHOR]
 */
class DepersonalizeActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { DepersonalizeItemsManager() }
}