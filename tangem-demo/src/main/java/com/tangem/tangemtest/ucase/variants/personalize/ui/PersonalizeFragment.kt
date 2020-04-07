package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tangem.CardManager
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.ParamsManagerFactory
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.CardError
import com.tangem.tangemtest.ucase.tunnel.ItemError
import com.tangem.tangemtest.ucase.ui.ActionViewModelFactory
import com.tangem.tangemtest.ucase.ui.BaseFragment
import com.tangem.tangemtest.ucase.ui.ParamsViewModel
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.PersonalizeItemBuilder
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class PersonalizeFragment : BaseFragment(), ActionView {

    private lateinit var itemContainer: ViewGroup
    private lateinit var actionFab: FloatingActionButton

    private val mainActivityVM: MainViewModel by activityViewModels()
    private val personalizeVM: PersonalizeViewModel by viewModels { PersonalizeViewModelFactory(PersonalizeConfig()) }
    private val paramsVM: ParamsViewModel by viewModels() { ActionViewModelFactory(itemsManager) }

    private val itemsManager: ItemsManager by lazy { ParamsManagerFactory.createFactory().get(ActionType.Personalize)!! }

    override fun getLayoutId(): Int = R.layout.fg_personalize

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemContainer = mainView.findViewById(R.id.ll_container)
        actionFab = mainView.findViewById(R.id.fab_action)

        paramsVM.setCardManager(CardManager.init(requireActivity()))
        paramsVM.attachToPayload(mutableMapOf(PayloadKey.actionView to this as ActionView))

        initFab()
        createWidgets { subscribeToViewModelChanges() }
    }

    private fun initFab() {
        actionFab.setOnClickListener { paramsVM.invokeMainAction() }
    }

    private fun createWidgets(widgetCreatedCallback: () -> Unit) {
        Log.d(this, "createWidgets")
        val blockList = mutableListOf<Item>()
        personalizeVM.ldBlockList.observe(viewLifecycleOwner, Observer { list ->
            Log.d(this, "ldBlockList size: ${list.size}")
            blockList.clear()
            blockList.addAll(list)
            blockList.forEach { WidgetBuilder(PersonalizeItemBuilder()).build(it, itemContainer) }
            paramsVM.attachToPayload(mutableMapOf(
                    PayloadKey.actionView to this as ActionView,
                    PayloadKey.itemList to blockList
            ))
            widgetCreatedCallback()
        })
    }

    private fun subscribeToViewModelChanges() {
        Log.d(this, "subscribeToViewModelChanges")
        listenEvent()
        listenError()
        listenDescriptionSwitchChanges()
    }

    private fun listenEvent() {
        paramsVM.seResponseEvent.observe(viewLifecycleOwner, Observer {
            mainActivityVM.changeResponseEvent(it)
            navigateTo(R.id.action_nav_card_action_to_response_screen)
        })
    }

    private fun listenError() {
        paramsVM.seError.observe(viewLifecycleOwner, Observer { showSnackbar(it) })

    }

    private fun listenDescriptionSwitchChanges() {
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            personalizeVM.toggleDescriptionVisibility(it)
        })
    }

    override fun enableActionFab(enable: Boolean) {
        if (enable) actionFab.show() else actionFab.hide()
    }

    override fun showSnackbar(id: Id) {
//        MainResourceHolder.safeGet<>()
        when (id) {
            CardError.NotPersonalized -> showSnackbar(R.string.card_error_not_personalized)
            ItemError.BadSeries -> showSnackbar(R.string.card_error_bad_series)
            ItemError.BadCardNumber -> showSnackbar(R.string.card_error_bad_series_number)
            else -> showSnackbar(requireContext().getString(R.string.unknown))
        }
    }
}







