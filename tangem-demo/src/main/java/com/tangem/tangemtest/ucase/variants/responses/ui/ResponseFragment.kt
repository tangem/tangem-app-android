package com.tangem.tangemtest.ucase.variants.responses.ui

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
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

        setHasOptionsMenu(true)
        setTittle()
    }

    private fun setTittle() {
        val titleId = getTittleId(arguments) ?: selfVM.determineTitleId(mainActivityVM.commandResponse)
        activity?.setTitle(titleId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        inflater.inflate(R.menu.menu_fg_response, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> {
                shareText(ResponseJsonConverter().convertResponse(mainActivityVM.commandResponse))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val argTittle = "tittle"

        fun setTittle(@StringRes id: Int): Bundle = bundleOf(Pair(argTittle, id))

        private fun getTittleId(args: Bundle?): Int? = args?.getInt(argTittle)
    }
}