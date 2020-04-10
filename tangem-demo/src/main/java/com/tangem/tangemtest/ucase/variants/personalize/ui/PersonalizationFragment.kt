package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.PersonalizationItemsManager
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.ItemError
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment
import com.tangem.tangemtest.ucase.variants.personalize.PersonalizationConfigStore
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.PersonalizationItemBuilder
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class PersonalizationFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { PersonalizationItemsManager(PersonalizationConfigStore(requireContext())) }

    override fun getLayoutId(): Int = R.layout.fg_personalization

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(itemsManager as PersonalizationItemsManager)
    }

    override fun initFab() {
        actionFab.setOnClickListener { actionVM.invokeMainAction() }
    }

    override fun createWidgets(widgetCreatedCallback: () -> Unit) {
        Log.d(this, "createWidgets")
        val itemList = mutableListOf<Item>()

        actionVM.ldItemList.observe(viewLifecycleOwner, Observer { list ->
            Log.d(this, "ldBlockList size: ${list.size}")
            itemList.clear()
            itemList.addAll(list)
            itemList.forEach { WidgetBuilder(PersonalizationItemBuilder()).build(it, itemContainer) }
            actionVM.attachToPayload(mutableMapOf(
                    PayloadKey.actionView to this as ActionView,
                    PayloadKey.itemList to itemList
            ))
            widgetCreatedCallback()
        })
    }

    override fun subscribeToViewModelChanges() {
        Log.d(this, "subscribeToViewModelChanges")
        listenEvent()
        listenError()
        listenDescriptionSwitchChanges()
    }

    override fun listenEvent() {
        actionVM.seResponseEvent.observe(viewLifecycleOwner, Observer {
            mainActivityVM.changeResponseEvent(it)
            navigateTo(R.id.action_nav_card_action_to_response_screen)
        })
    }

    override fun showSnackbar(id: Id, additionalHandler: ((Id) -> Int)?) {
        super.showSnackbar(id) {
            when (id) {
                ItemError.BadSeries -> R.string.card_error_bad_series
                ItemError.BadCardNumber -> R.string.card_error_bad_series_number
                else -> additionalHandler?.invoke(id) ?: UNDEFINED
            }
        }
    }
}