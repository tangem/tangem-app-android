package com.tangem.tangemtest.ucase.variants.responses.ui.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.DescriptionWidget
import ru.dev.gbixahue.eu4d.lib.android._android.views.stringFrom
import ru.dev.gbixahue.eu4d.lib.android._android.views.toast
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
abstract class ResponseWidget(parent: ViewGroup, item: Item): DescriptionWidget(parent, item)  {

    init {
        view.setOnClickListener {
            val data = stringOf(item.getData<Any?>())
            val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: return@setOnClickListener

            val clip: ClipData = ClipData.newPlainText("FieldValue", "$data")
            clipboard.setPrimaryClip(clip)

            val copyMessage = view.stringFrom(R.string.copy_to_clipboard)
            view.toast("$copyMessage: ${getName()} - $data")
        }
    }
}