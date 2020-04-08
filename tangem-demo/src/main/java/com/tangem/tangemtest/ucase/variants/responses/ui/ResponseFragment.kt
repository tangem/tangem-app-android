package com.tangem.tangemtest.ucase.variants.responses.ui

import android.os.Bundle
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
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
open class ResponseFragment : BaseFragment() {

    companion object Arg {
        val response = "response"
    }

    private val mainActivityVM: MainViewModel by activityViewModels()
    private val selfVM: ResponseViewModel by viewModels()

    private val itemContainer: ViewGroup by lazy { mainView.findViewById<LinearLayout>(R.id.ll_container) }

    override fun getLayoutId(): Int = R.layout.fg_card_response

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivityVM.ldResponseEvent.observe(viewLifecycleOwner, Observer { event ->
            val builder = WidgetBuilder(ResponseItemBuilder())
            val itemList = selfVM.createItemList(event)
            itemList.forEach { builder.build(it, itemContainer) }
        })
        subscribeToViewModelChanges()
    }

    private fun subscribeToViewModelChanges() {
        Log.d(this, "subscribeToViewModelChanges")
        listenDescriptionSwitchChanges()
    }


    private fun listenDescriptionSwitchChanges() {
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            selfVM.toggleDescriptionVisibility(it)
        })
    }
}
