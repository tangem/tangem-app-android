package com.tangem.devkit.ucase.variants.issuerdata.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.WriteIssuerExtraDataItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

class WriteIssuerExtraDataFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { WriteIssuerExtraDataItemsManager() }
}