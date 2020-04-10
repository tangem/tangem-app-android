package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets

import android.view.ViewGroup
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.widget.abstraction.BaseViewWidget
import com.tangem.tangemtest._arch.widget.abstraction.ViewWidget
import com.tangem.tangemtest.ucase.resources.MainResourceHolder
import com.tangem.tangemtest.ucase.resources.Resources
import ru.dev.gbixahue.eu4d.lib.android._android.views.stringFrom

/**
[REDACTED_AUTHOR]
 */
abstract class BaseAppWidget(parent: ViewGroup, item: Item) : BaseViewWidget(parent, item) {
    override fun getName(): String {
        val idName = if (item.id is StringId) (item.id as StringId).value else ""

        val resources = getResources() ?: return idName
        return view.stringFrom(resources.resName)
    }
}

fun ViewWidget.getResources(): Resources? = MainResourceHolder.get(item.id)

fun ViewWidget.getResNameId(): Int = MainResourceHolder.safeGet<Resources>(item.id).resName

fun ViewWidget.getResDescriptionId(): Int? = MainResourceHolder.get(item.id)?.resDescription