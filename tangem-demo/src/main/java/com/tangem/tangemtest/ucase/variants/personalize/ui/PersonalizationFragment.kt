package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.Observer
import com.tangem.commands.Card
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest.commons.view.ButtonState
import com.tangem.tangemtest.commons.view.MultiActionView
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.PersonalizationItemsManager
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.ItemError
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment
import com.tangem.tangemtest.ucase.variants.personalize.PersonalizationConfigStore
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.PersonalizationItemBuilder
import ru.dev.gbixahue.eu4d.lib.android._android.views.inflate
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

    override fun widgetsWasCreated() {
        super.widgetsWasCreated()

        val btnContainer = itemContainer.inflate<ViewGroup>(R.layout.view_simple_button)
        val btn = btnContainer.findViewById<Button>(R.id.button)

        val show = StringId("show")
        val hide = StringId("hide")
        val multiAction = MultiActionView(mutableListOf(
                ButtonState(show, R.string.show_rare_fields) { actionVM.showFields(ActionType.Personalize) },
                ButtonState(hide, R.string.hide_rare_fields) { actionVM.hideFields(ActionType.Personalize) }
        ), btn)
        multiAction.afterAction = {
            multiAction.state = if (it == show) hide else show
        }
        multiAction.performAction(hide)
        itemContainer.addView(btnContainer)
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

    override fun showSnackbar(id: Id, additionalHandler: ((Id) -> Int)?) {
        super.showSnackbar(id) {
            when (id) {
                ItemError.BadSeries -> R.string.card_error_bad_series
                ItemError.BadCardNumber -> R.string.card_error_bad_series_number
                else -> additionalHandler?.invoke(id) ?: UNDEFINED
            }
        }
    }

    override fun responseCardDataHandled(card: Card?) {
        super.responseCardDataHandled(card)
        navigateTo(R.id.action_nav_card_action_to_response_screen)
    }
}