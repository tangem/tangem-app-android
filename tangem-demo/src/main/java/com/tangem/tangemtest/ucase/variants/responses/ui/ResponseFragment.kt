package com.tangem.tangemtest.ucase.variants.responses.ui

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.extensions.shareText
import com.tangem.tangemtest.ucase.domain.responses.ResponseJsonConverter
import com.tangem.tangemtest.ucase.ui.BaseFragment
import com.tangem.tangemtest.ucase.variants.responses.ResponseViewModel
import com.tangem.tangemtest.ucase.variants.responses.ui.widget.ResponseItemBuilder

/**
[REDACTED_AUTHOR]
 */
open class ResponseFragment : BaseFragment() {

    private val mainActivityVM: MainViewModel by activityViewModels()
    private val selfVM: ResponseViewModel by viewModels()

    private val itemContainer: ViewGroup by lazy { mainView.findViewById<LinearLayout>(R.id.ll_content_container) }

    override fun getLayoutId(): Int = R.layout.fg_card_response

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTittle()
    }

    private fun setTittle() {
        val titleId = selfVM.determineTitleId(mainActivityVM.commandResponse)
        activity?.setTitle(titleId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        buildWidgets()
        listenDescriptionSwitchChanges()
    }

    private fun buildWidgets() {
        val builder = WidgetBuilder(ResponseItemBuilder())
        val itemList = selfVM.createItemList(mainActivityVM.commandResponse)
        itemList.forEach { builder.build(it, itemContainer) }
    }

    private fun listenDescriptionSwitchChanges() {
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            selfVM.toggleDescriptionVisibility(it)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        val menuItem = menu.findItem(R.id.action_share)
        menuItem.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> {
                shareText(ResponseJsonConverter().convertResponse(mainActivityVM.commandResponse))
            }
        }
        return super.onOptionsItemSelected(item)
    }
}