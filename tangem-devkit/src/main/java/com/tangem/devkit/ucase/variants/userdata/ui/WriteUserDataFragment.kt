package com.tangem.devkit.ucase.variants.userdata.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.WriteUserDataItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

class WriteUserDataFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { WriteUserDataItemsManager() }
}