package com.tangem.tangemtest.ucase.variants.responses.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.ucase.ui.BaseFragment
import com.tangem.tangemtest.ucase.variants.responses.ResponseViewModel
import com.tangem.tangemtest.ucase.variants.responses.ui.widget.ResponseItemBuilder

/**
[REDACTED_AUTHOR]
 */
open class ResponseFragment : BaseFragment() {

    private val mainActivityVM: MainViewModel by activityViewModels()
    private val selfVM: ResponseViewModel by viewModels()

    private val itemContainer: ViewGroup by lazy { mainView.findViewById<LinearLayout>(R.id.ll_container) }

    override fun getLayoutId(): Int = R.layout.fg_card_response

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setTittle()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setTittle() {
        val titleId = selfVM.determineTitleId(mainActivityVM.responseEvent)
        activity?.setTitle(titleId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buildWidgets()
        listenDescriptionSwitchChanges()
    }

    private fun buildWidgets() {
        val builder = WidgetBuilder(ResponseItemBuilder())
        val itemList = selfVM.createItemList(mainActivityVM.responseEvent)
        itemList.forEach { builder.build(it, itemContainer) }
    }

    private fun listenDescriptionSwitchChanges() {
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            selfVM.toggleDescriptionVisibility(it)
        })
    }
}
