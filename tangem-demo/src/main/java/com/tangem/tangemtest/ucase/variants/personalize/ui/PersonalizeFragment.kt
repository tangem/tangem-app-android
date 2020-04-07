package com.tangem.tangemtest.ucase.variants.personalize.ui

import androidx.lifecycle.Observer
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.CardError
import com.tangem.tangemtest.ucase.tunnel.ItemError
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.PersonalizeItemBuilder
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class PersonalizeFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_personalize

    override fun initFab() {
        actionFab.setOnClickListener { paramsVM.invokeMainAction() }
    }

    override fun createWidgets(widgetCreatedCallback: () -> Unit) {
        Log.d(this, "createWidgets")
        val itemList = mutableListOf<Item>()

        paramsVM.ldItemList.observe(viewLifecycleOwner, Observer { list ->
            Log.d(this, "ldBlockList size: ${list.size}")
            itemList.clear()
            itemList.addAll(list)
            itemList.forEach { WidgetBuilder(PersonalizeItemBuilder()).build(it, itemContainer) }
            paramsVM.attachToPayload(mutableMapOf(
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
        paramsVM.seResponseEvent.observe(viewLifecycleOwner, Observer {
            mainActivityVM.changeResponseEvent(it)
            navigateTo(R.id.action_nav_card_action_to_response_screen)
        })
    }

    override fun showSnackbar(id: Id) {
        when (id) {
            CardError.NotPersonalized -> showSnackbar(R.string.card_error_not_personalized)
            ItemError.BadSeries -> showSnackbar(R.string.card_error_bad_series)
            ItemError.BadCardNumber -> showSnackbar(R.string.card_error_bad_series_number)
            else -> showSnackbar(requireContext().getString(R.string.unknown))
        }
    }

    override fun getAction(): ActionType = ActionType.Personalize
}







