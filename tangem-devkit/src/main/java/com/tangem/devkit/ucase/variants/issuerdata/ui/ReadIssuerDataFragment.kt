package com.tangem.devkit.ucase.variants.issuerdata.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.ReadIssuerDataItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

class ReadIssuerDataFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { ReadIssuerDataItemsManager() }
}