package com.tangem.devkit.ucase.variants.depersonalize.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.DepersonalizeItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

/**
[REDACTED_AUTHOR]
 */
class DepersonalizeActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { DepersonalizeItemsManager() }
}