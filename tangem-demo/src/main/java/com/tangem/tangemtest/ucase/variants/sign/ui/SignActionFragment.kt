package com.tangem.tangemtest.ucase.variants.sign.ui

import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.SignItemsManager
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment

/**
[REDACTED_AUTHOR]
 */
class SignActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { SignItemsManager() }
}