package com.tangem.tangemtest.ucase.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ListItemBlock
import com.tangem.tangemtest._arch.structure.abstraction.iterate
import com.tangem.tangemtest._arch.structure.impl.TextItem
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.ucase.resources.StringId

/**
[REDACTED_AUTHOR]
 */
class BaseCardResponseFragment : BaseFragment() {

    companion object Arg {
        val response = "response"
    }

    private val mainActivityVM: MainViewModel by activityViewModels()
    private val blockContainer: ViewGroup by lazy { mainView.findViewById<LinearLayout>(R.id.ll_container) }

    override fun getLayoutId(): Int = R.layout.fg_card_response

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val response = requireArguments().getString(Arg.response) ?: return
        if (response.isEmpty()) return

        val itemList = createItemList(response)
        itemList.forEach { WidgetBuilder(ResponseItemBuilder()).build(it, blockContainer) }

        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            itemList.iterate { item ->
                val baseItem = item as? BaseItem<*> ?: return@iterate
                baseItem.viewModel.viewState.descriptionVisibility = if (it) View.VISIBLE else View.GONE
            }
        })

    }

    protected open fun createItemList(response: String): List<Item> {
        return listOf(
                ListItemBlock(StringId("")).apply {
                    addItem(TextItem(StringId("resp.fieldName"), response))
                }
        )
    }
}
