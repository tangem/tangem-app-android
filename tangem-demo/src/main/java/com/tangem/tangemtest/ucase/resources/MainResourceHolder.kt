package com.tangem.tangemtest.ucase.resources

import com.tangem.tangemtest.AppTangemDemo
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.StringResId
import com.tangem.tangemtest.ucase.resources.initializers.ActionResources
import com.tangem.tangemtest.ucase.resources.initializers.PersonalizationResources
import com.tangem.tangemtest.ucase.resources.initializers.ResponseResources
import com.tangem.tangemtest.ucase.resources.initializers.TlvResources
import ru.dev.gbixahue.eu4d.lib.kotlin.common.BaseTypedHolder

/**
[REDACTED_AUTHOR]
 */
open class ResourceHolder<T> : BaseTypedHolder<T, Resources>()

object MainResourceHolder : ResourceHolder<Id>() {
    init {
        PersonalizationResources().init(this)
        ActionResources().init(this)
        TlvResources().init(this)
        ResponseResources().init(this)
    }

    override fun get(type: Id): Resources? {
        return when(type) {
            is StringId -> getResourcesFromName(type.value)
            is StringResId -> Resources(type.value)
            else -> super.get(type)
        }
    }

    private fun getResourcesFromName(resName: String): Resources? {
        val context = AppTangemDemo.appInstance.applicationContext
        val resNameId = context.resources.getIdentifier(resName, "string", context.packageName)
        val resInfoId = context.resources.getIdentifier("info_$resName", "string", context.packageName)
        if (resNameId == 0) return null

        return Resources(resNameId, if (resInfoId == 0) null else resInfoId)
    }

    inline fun <reified Res : Resources> safeGet(id: Id): Res {
        val result = super.get(id) ?: Resources(R.string.unknown, R.string.unknown)
        return result as Res
    }
}