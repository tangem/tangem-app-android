package com.tangem.devkit.ucase.variants.userdata.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.WriteProtectedUserDataItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

class WriteProtectedUserDataFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { WriteProtectedUserDataItemsManager() }
}