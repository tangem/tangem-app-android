package com.tangem.devkit.ucase.variants.sign.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.SignItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

/**
[REDACTED_AUTHOR]
 */
class SignActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { SignItemsManager() }
}