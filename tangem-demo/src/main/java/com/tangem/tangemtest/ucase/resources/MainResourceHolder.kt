package com.tangem.tangemtest.ucase.resources

import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest.ucase.resources.initializers.ActionResources
import com.tangem.tangemtest.ucase.resources.initializers.PersonalizeResources
import ru.dev.gbixahue.eu4d.lib.kotlin.common.BaseTypedHolder

/**
[REDACTED_AUTHOR]
 */
open class ResourceHolder<T> : BaseTypedHolder<T, Resources>()

object MainResourceHolder : ResourceHolder<Id>() {
    init {
        PersonalizeResources().init(this)
        ActionResources().init(this)
    }

    inline fun <reified Res : Resources> safeGet(id: Id): Res {
        val result = super.get(id) ?: Resources(R.string.unknown, R.string.unknown)
        return result as Res
    }
}
