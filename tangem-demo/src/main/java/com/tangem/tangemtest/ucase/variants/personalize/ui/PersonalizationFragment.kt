package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.tangem.commands.Card
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest.commons.view.MultiActionView
import com.tangem.tangemtest.commons.view.ViewAction
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
import ru.dev.gbixahue.eu4d.lib.android.global.threading.postUI
import ru.dev.gbixahue.eu4d.lib.android.global.threading.postWork

/**
[REDACTED_AUTHOR]
 */
class PersonalizationFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { PersonalizationItemsManager(PersonalizationConfigStore(requireContext())) }

    override fun getLayoutId(): Int = R.layout.fg_base_action_layout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(itemsManager as PersonalizationItemsManager)
    }

    override fun bindViews() {
        super.bindViews()
        swrLayout.isRefreshing = true
    }

    override fun initViews() {
        actionFab.setOnClickListener { actionVM.invokeMainAction() }
    }

    override fun createWidgets(widgetCreatedCallback: () -> Unit) {
        Log.d(this, "createWidgets")
        val itemList = mutableListOf<Item>()

        val maxDelay = 500
        val timeStart = System.currentTimeMillis()
        actionVM.ldItemList.observe(viewLifecycleOwner, Observer { list ->
            Log.d(this, "ldBlockList size: ${list.size}")
            itemList.addAll(list)
            val llContainer = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            postWork {
                val builder = WidgetBuilder(PersonalizationItemBuilder())
                itemList.forEach { builder.build(it, llContainer) }
                actionVM.attachToPayload(mutableMapOf(
                        PayloadKey.actionView to this as ActionView,
                        PayloadKey.itemList to itemList
                ))
                val timeEnd = System.currentTimeMillis()
                val diff = timeEnd - timeStart
                postUI(maxDelay - diff) {
                    TransitionManager.beginDelayedTransition(contentContainer, Fade())
                    contentContainer.addView(llContainer)
                    widgetCreatedCallback()
                    swrLayout.isRefreshing = false
                    swrLayout.isEnabled = false
                }
            }
        })
    }

    override fun widgetsWasCreated() {
        super.widgetsWasCreated()

        val btnContainer = contentContainer.inflate<ViewGroup>(R.layout.view_simple_button)
        val btn = btnContainer.findViewById<Button>(R.id.button)

        val show = StringId("show")
        val hide = StringId("hide")
        val multiAction = MultiActionView(mutableListOf(
                ViewAction(show, R.string.show_rare_fields) { actionVM.showFields(ActionType.Personalize) },
                ViewAction(hide, R.string.hide_rare_fields) { actionVM.hideFields(ActionType.Personalize) }
        ), btn)
        multiAction.afterAction = {
            multiAction.state = if (it == show) hide else show
        }
        multiAction.performAction(hide)
        contentContainer.addView(btnContainer)
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