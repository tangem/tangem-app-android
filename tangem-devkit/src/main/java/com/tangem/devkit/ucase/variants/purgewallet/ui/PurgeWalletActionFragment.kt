package com.tangem.devkit.ucase.variants.purgewallet.ui

import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.PurgeWalletItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment

class PurgeWalletActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { PurgeWalletItemsManager() }
}