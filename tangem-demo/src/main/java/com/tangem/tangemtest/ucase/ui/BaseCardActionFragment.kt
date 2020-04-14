package com.tangem.tangemtest.ucase.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.plusAssign
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tangem.TangemSdk
import com.tangem.commands.Card
import com.tangem.commands.CommandResponse
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.CardError
import com.tangem.tangemtest.ucase.ui.widgets.ParameterWidget
import ru.dev.gbixahue.eu4d.lib.android._android.views.enable
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
abstract class BaseCardActionFragment : BaseFragment(), ActionView {

    protected lateinit var swrLayout: SwipeRefreshLayout
    protected lateinit var contentContainer: ViewGroup
    protected lateinit var actionFab: FloatingActionButton

    protected abstract val itemsManager: ItemsManager
    protected val mainActivityVM by activityViewModels<MainViewModel>()
    protected val actionVM: ActionViewModel by viewModels { ActionViewModelFactory(itemsManager) }
    protected val UNDEFINED = -1

    private val paramsWidgetList = mutableListOf<ParameterWidget>()

    override fun getLayoutId(): Int = R.layout.fg_base_action_layout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(this, "onViewCreated")

        bindViews()
        viewLifecycleOwner.lifecycle.addObserver(actionVM)
        actionVM.setCardManager(TangemSdk.init(requireActivity()))
        actionVM.attachToPayload(mutableMapOf(PayloadKey.actionView to this as ActionView))

        initViews()
        createWidgets {
            widgetsWasCreated()
            subscribeToViewModelChanges()
        }
    }

    protected open fun widgetsWasCreated() {}

    protected open fun bindViews() {
        swrLayout = mainView.findViewById(R.id.swr_layout)
        contentContainer = mainView.findViewById(R.id.ll_content_container)
        actionFab = mainView.findViewById(R.id.fab_action)
    }

    protected open fun initViews() {
        swrLayout.isEnabled = false
        enableActionFab(false)
        actionFab.setOnClickListener { actionVM.invokeMainAction() }
    }

    protected open fun createWidgets(widgetCreatedCallback: () -> Unit) {
        Log.d(this, "createWidgets")
        actionVM.ldItemList.observe(viewLifecycleOwner, Observer { itemList ->
            itemList.forEach { param ->
                val widget = ParameterWidget(inflateParamView(contentContainer), param)
                widget.onValueChanged = { id, value -> actionVM.userChangedItem(id, value) }
                widget.onActionBtnClickListener = actionVM.getItemAction(param.id)
                paramsWidgetList.add(widget)
            }
            widgetCreatedCallback()
        })
    }

    protected open fun subscribeToViewModelChanges() {
        Log.d(this, "subscribeToViewModelChanges")
        actionVM.seResponse.observe(viewLifecycleOwner, Observer {
            Log.d(this, "handle response: $it")
            handleResponse(it)
        })
        actionVM.seResponseData.observe(viewLifecycleOwner, Observer {
            Log.d(this, "handle responseData: $it")
            handleResponseData(it)
        })
        actionVM.seResponseCardData.observe(viewLifecycleOwner, Observer {
            Log.d(this, "handle responseCardData: $it")
            handleResponseCardData(it)
        })
        actionVM.seError.observe(viewLifecycleOwner, Observer {
            Log.d(this, "handle error: $it")
            handleError(it)
        })
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            Log.d(this, "handle descriptionVisibilityState: $it")
            handleDescriptionSwitchChanges(it)
        })
        listenChangedItems()
    }

    protected open fun handleResponse(response: CommandResponse) {
        mainActivityVM.changeResponseEvent(response)
    }

    protected open fun handleResponseData(response: CommandResponse) {
        navigateTo(R.id.action_nav_card_action_to_response_screen, options = null)
    }

    protected open fun handleResponseCardData(card: Card) {}

    protected open fun handleError(error: String) {
        showSnackbar(error)
    }

    protected open fun handleDescriptionSwitchChanges(descriptionVisibilityState: Boolean) {
        actionVM.toggleDescriptionVisibility(descriptionVisibilityState)
    }

    @Deprecated("Start to use itemViewModel")
    protected open fun listenChangedItems() {
        actionVM.seChangedItems.observe(viewLifecycleOwner, Observer { itemList ->
            itemList.forEach { item ->
                Log.d(this, "item changed from VM - name: ${item.id}, value:${item.viewModel.data}")
                paramsWidgetList.firstOrNull { it.id == item.id }?.changeParamValue(item.viewModel.data)
            }
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

    override fun showSnackbar(id: Id, additionalHandler: ((Id) -> Int)?) {
        val resourceId = when (id) {
            CardError.NotPersonalized -> R.string.card_error_not_personalized
            else -> additionalHandler?.invoke(id) ?: UNDEFINED
        }

        if (resourceId != UNDEFINED) showSnackbar(resourceId)
    }
}