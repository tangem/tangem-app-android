package com.tangem.tangemtest.ucase.domain.paramsManager

import com.tangem.tangemtest.ucase.domain.paramsManager.managers.DepersonalizeItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.PersonalizeItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.ScanItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.SignItemsManager
import com.tangem.tangemtest.ucase.resources.ActionType
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.kotlin.common.BaseTypedHolder

/**
[REDACTED_AUTHOR]
 */
class ItemManagersStore : BaseTypedHolder<ActionType, ItemsManager>() {

    companion object {
        val instance: ItemManagersStore
            get() = createFactory()

        private fun createFactory(): ItemManagersStore {
            Log.d(this, "New instance was created")

            return ItemManagersStore().apply {
                register(ActionType.Scan, ScanItemsManager())
                register(ActionType.Sign, SignItemsManager())
                register(ActionType.Personalize, PersonalizeItemsManager())
                register(ActionType.Depersonalize, DepersonalizeItemsManager())
            }
        }
    }
}