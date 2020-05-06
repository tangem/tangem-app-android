package com.tangem.devkit.ucase.variants.responses.ui.widget

import android.view.ViewGroup
import com.tangem.devkit.R
import com.tangem.devkit._arch.structure.abstraction.Item
import com.tangem.devkit.extensions.copyToClipboard
import com.tangem.devkit.ucase.variants.personalize.ui.widgets.DescriptionWidget
import ru.dev.gbixahue.eu4d.lib.android._android.views.stringFrom
import ru.dev.gbixahue.eu4d.lib.android._android.views.toast
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
abstract class ResponseWidget(parent: ViewGroup, item: Item) : DescriptionWidget(parent, item) {

    init {
        view.setOnClickListener {
            val data = stringOf(item.getData<Any?>())
            val clipboardData = "${getName()} - $data"
            view.context.copyToClipboard(clipboardData, "FieldValue")

            val copyMessage = view.stringFrom(R.string.copy_to_clipboard)
            view.toast("$copyMessage\n$clipboardData")
        }
    }
}