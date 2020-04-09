package com.tangem.tangemtest.ucase.variants.responses

import android.view.View
import androidx.lifecycle.ViewModel
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ModelToItems
import com.tangem.tangemtest._arch.structure.abstraction.iterate
import com.tangem.tangemtest.ucase.variants.responses.converter.ConvertersStore
import com.tangem.tasks.TaskEvent
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class ResponseViewModel : ViewModel() {

    private val convertersHolder = ConvertersStore()
    private var itemList: List<Item>? = null

    fun createItemList(taskEvent: TaskEvent<*>): List<Item> {
        Log.d(this, "createItemList: itemList size: ${itemList?.size ?: 0}")

        val event = taskEvent as? TaskEvent.Event<Any> ?: return emptyList()
        val type = event.data::class.java
        val converter = convertersHolder.get(type) as? ModelToItems<Any> ?: return emptyList()
        itemList = converter.convert(event.data)
        return itemList!!
    }

    fun toggleDescriptionVisibility(state: Boolean) {
        itemList?.iterate {
            it.viewModel.viewState.descriptionVisibility = if (state) View.VISIBLE else View.GONE
        }
    }
}




