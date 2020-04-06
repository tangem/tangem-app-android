package com.tangem.tangemtest.ucase.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.tangem.CardManager
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.ParamsManagerFactory
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.CardError
import com.tangem.tangemtest.ucase.ui.widgets.ParameterWidget
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.kotlin.common.LayoutHolder

/**
[REDACTED_AUTHOR]
 */
abstract class BaseCardActionFragment : Fragment(), LayoutHolder, ActionView {

    protected val incomingParamsContainer: ViewGroup by lazy {
        mainView.findViewById<LinearLayout>(R.id.ll_incoming_params_container)
    }
    protected lateinit var mainView: View
    protected val actionFab: FloatingActionButton by lazy { mainView.findViewById<FloatingActionButton>(R.id.fab_action) }

    protected val paramsVM: ParamsViewModel by viewModels { ActionViewModelFactory(itemsManager) }
    protected val widgetList = mutableListOf<ParameterWidget>()

    private val itemsManager: ItemsManager by lazy { ParamsManagerFactory.createFactory().get(getAction())!! }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(this, "onCreateView")
        mainView = inflater.inflate(getLayoutId(), container, false)
        return mainView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(this, "onViewCreated")

        viewLifecycleOwner.lifecycle.addObserver(paramsVM)
        paramsVM.setCardManager(CardManager.init(requireActivity()))
        paramsVM.attachToPayload(mutableMapOf(PayloadKey.actionView to this as ActionView))
        initFab()
        showActionFab(false)
        createWidgets { subscribeToViewModelChanges() }
    }

    private fun initFab() {
        Log.d(this, "initFab")
        actionFab.setOnClickListener { paramsVM.invokeMainAction() }
    }

    private fun createWidgets(widgetCreatedCallback: () -> Unit) {
        Log.d(this, "createWidgets")
        paramsVM.ldParams.observe(viewLifecycleOwner, Observer { itemList ->
            itemList.forEach { param ->
                val widget = ParameterWidget(inflateParamView(incomingParamsContainer), param)
                widget.onValueChanged = { id, value -> paramsVM.userChangedItem(id, value) }
                widget.onActionBtnClickListener = paramsVM.getItemAction(param.id)
                widgetList.add(widget)
            }
            widgetCreatedCallback()
        })
    }

    private fun subscribeToViewModelChanges() {
        Log.d(this, "subscribeToViewModelChanges")
        listenResponse()
        listenError()
        listenChangedItems()
        listenDescriptionSwitchChanges()
    }

    protected open fun listenResponse() {
        val tvResponse by lazy { mainView.findViewById<TextView>(R.id.tv_action_response_json) }
        paramsVM.ldResponse.observe(viewLifecycleOwner, Observer {
            Log.d(this, "action response: ${if (it.length > 50) it.substring(0..50) else it}")
            tvResponse.text = it
        })
    }

    protected open fun listenError() {
        paramsVM.seError.observe(viewLifecycleOwner, Observer { showSnackbar(it) })
    }

    protected open fun listenChangedItems() {
        paramsVM.seChangedItems.observe(viewLifecycleOwner, Observer { itemList ->
            itemList.forEach { item ->
                Log.d(this, "item changed from VM - name: ${item.id}")
                val dataItem = item as? BaseItem<Any?> ?: return@Observer
                Log.d(this, "item changed from VM - name: ${dataItem.id}, value:${dataItem.viewModel.data}")
                widgetList.firstOrNull { it.id == item.id }?.changeParamValue(item.viewModel.data)
            }
        })
    }

    protected open fun listenDescriptionSwitchChanges() {
        val mainActViewModel by activityViewModels<MainViewModel>()
        mainActViewModel.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer { isEnabled ->
            widgetList.forEach { it.toggleDescriptionVisibility(isEnabled) }
        })
    }

    private fun inflateParamView(where: ViewGroup): ViewGroup {
        val inflater = LayoutInflater.from(where.context)
        val view = inflater.inflate(R.layout.w_card_incoming_param, where, false)
        where.addView(view)
        return view as ViewGroup
    }

    override fun showActionFab(show: Boolean) {
        if (show) actionFab.show() else actionFab.hide()
    }

    override fun showSnackbar(id: Id) {
//        MainResourceHolder.safeGet<>()
        when (id) {
            CardError.NotPersonalized -> showSnackbar(R.string.card_error_not_personalized)
            else -> showSnackbar(requireContext().getString(R.string.unknown))
        }
    }

    override fun showSnackbar(id: Int) {
        showSnackbar(requireContext().getString(id))
    }

    override fun showSnackbar(message: String) {
        Snackbar.make(mainView, message, BaseTransientBottomBar.LENGTH_SHORT).show()
    }

    abstract fun getAction(): ActionType
}