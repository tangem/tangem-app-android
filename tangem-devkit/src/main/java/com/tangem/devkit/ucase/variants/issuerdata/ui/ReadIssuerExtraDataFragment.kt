package com.tangem.devkit.ucase.variants.issuerdata.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.ReadIssuerExtraDataItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

class ReadIssuerExtraDataFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { ReadIssuerExtraDataItemsManager() }
}