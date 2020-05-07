package com.tangem.devkit.ucase.variants.createwallet.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.CreateWalletItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

class CreateWalletActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { CreateWalletItemsManager() }
}