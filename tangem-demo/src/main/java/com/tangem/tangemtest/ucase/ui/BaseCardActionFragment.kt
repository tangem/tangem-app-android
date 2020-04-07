package com.tangem.tangemtest.ucase.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.plusAssign
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tangem.CardManager
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemManagersStore
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.CardError
import com.tangem.tangemtest.ucase.ui.widgets.ParameterWidget
import ru.dev.gbixahue.eu4d.lib.android._android.views.enable
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
abstract class BaseCardActionFragment : BaseFragment(), ActionView {

    protected val itemContainer: ViewGroup
        get() = mainView.findViewById(R.id.ll_container)

    protected val actionFab: FloatingActionButton
        get() = mainView.findViewById(R.id.fab_action)

    protected val mainActivityVM by activityViewModels<MainViewModel>()
    protected val actionVM: ActionViewModel by viewModels { ActionViewModelFactory(itemsManager) }

    protected val itemsManager: ItemsManager by lazy {
        Log.d(this, "create ItemManager")
        ItemManagersStore.instance.get(getAction())!!
    }

    private val paramsWidgetList = mutableListOf<ParameterWidget>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(this, "onViewCreated")

        viewLifecycleOwner.lifecycle.addObserver(actionVM)
        actionVM.setCardManager(CardManager.init(requireActivity()))
        actionVM.attachToPayload(mutableMapOf(PayloadKey.actionView to this as ActionView))

        initFab()
        createWidgets { subscribeToViewModelChanges() }
    }

    protected open fun initFab() {
        enableActionFab(false)
        actionFab.setOnClickListener { actionVM.invokeMainAction() }
    }

    protected open fun createWidgets(widgetCreatedCallback: () -> Unit) {
        Log.d(this, "createWidgets")
        actionVM.ldItemList.observe(viewLifecycleOwner, Observer { itemList ->
            itemList.forEach { param ->
                val widget = ParameterWidget(inflateParamView(itemContainer), param)
                widget.onValueChanged = { id, value -> actionVM.userChangedItem(id, value) }
                widget.onActionBtnClickListener = actionVM.getItemAction(param.id)
                paramsWidgetList.add(widget)
            }
            widgetCreatedCallback()
        })
    }

    protected open fun subscribeToViewModelChanges() {
        Log.d(this, "subscribeToViewModelChanges")
        listenEvent()
        listenReadResponse()
        listenResponse()
        listenError()
        listenChangedItems()
        listenDescriptionSwitchChanges()
    }

    protected open fun listenEvent() {
        actionVM.seResponseEvent.observe(viewLifecycleOwner, Observer {
            mainActivityVM.changeResponseEvent(it)
        })
    }

    protected open fun listenReadResponse() {}

    protected open fun listenResponse() {
        actionVM.seResponse.observe(viewLifecycleOwner, Observer {
            navigateTo(R.id.action_nav_card_action_to_response_screen)
        })
    }

    protected open fun listenError() {
        actionVM.seError.observe(viewLifecycleOwner, Observer { showSnackbar(it) })
    }

    @Deprecated("Start to use itemViewModel")
    protected open fun listenChangedItems() {
        actionVM.seChangedItems.observe(viewLifecycleOwner, Observer { itemList ->
            itemList.forEach { item ->
                Log.d(this, "item changed from VM - name: ${item.id}")
                val dataItem = item as? BaseItem<Any?> ?: return@Observer
                Log.d(this, "item changed from VM - name: ${dataItem.id}, value:${dataItem.viewModel.data}")
                paramsWidgetList.firstOrNull { it.id == item.id }?.changeParamValue(item.viewModel.data)
            }
        })
    }

    protected open fun listenDescriptionSwitchChanges() {
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            actionVM.toggleDescriptionVisibility(it)
        })
    }

    private fun inflateParamView(where: ViewGroup): ViewGroup {
        val inflater = LayoutInflater.from(where.context)
        val view = inflater.inflate(R.layout.w_card_incoming_param, where, false)
        where.plusAssign(view)
        return view as ViewGroup
    }

    override fun enableActionFab(enable: Boolean) {
        actionFab.enable(enable)
    }

    override fun showSnackbar(id: Id) {
//        MainResourceHolder.safeGet<>()
        when (id) {
            CardError.NotPersonalized -> showSnackbar(R.string.card_error_not_personalized)
            else -> showSnackbar(requireContext().getString(R.string.unknown))
        }
    }

    abstract fun getAction(): ActionType
}