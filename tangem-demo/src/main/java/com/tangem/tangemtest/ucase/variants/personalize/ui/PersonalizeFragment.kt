package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.tangem.tangemtest.AppTangemDemo
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.PersonalizeItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.Store
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.CardError
import com.tangem.tangemtest.ucase.tunnel.ItemError
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.PersonalizeItemBuilder
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class PersonalizeFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { PersonalizeItemsManager(PersonalizationConfigStore(requireContext())) }

    override fun getLayoutId(): Int = R.layout.fg_personalize

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(itemsManager as PersonalizeItemsManager)
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
            itemList.forEach { WidgetBuilder(PersonalizeItemBuilder()).build(it, itemContainer) }
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

    override fun showSnackbar(id: Id) {
        when (id) {
            CardError.NotPersonalized -> showSnackbar(R.string.card_error_not_personalized)
            ItemError.BadSeries -> showSnackbar(R.string.card_error_bad_series)
            ItemError.BadCardNumber -> showSnackbar(R.string.card_error_bad_series_number)
            else -> showSnackbar(requireContext().getString(R.string.unknown))
        }
    }
}

class PersonalizationConfigStore(context: Context) : Store<PersonalizeConfig> {

    private val key = "personalization_config"

    private val sp: SharedPreferences = (context.applicationContext as AppTangemDemo).sharedPreferences()
    private val gson: Gson = Gson()

    override fun save(config: PersonalizeConfig) {
        sp.edit(true) { putString(key, gson.toJson(config)) }
    }

    override fun restore(): PersonalizeConfig {
        val json = sp.getString(key, gson.toJson(PersonalizeConfig()))
        return gson.fromJson(json, PersonalizeConfig::class.java)
    }
}






