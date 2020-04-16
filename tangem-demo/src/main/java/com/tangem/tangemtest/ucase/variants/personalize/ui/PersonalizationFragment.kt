package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import com.tangem.commands.Card
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.SafeValueChanged
import com.tangem.tangemtest._arch.widget.WidgetBuilder
import com.tangem.tangemtest.commons.DialogController
import com.tangem.tangemtest.commons.view.MultiActionView
import com.tangem.tangemtest.commons.view.ViewAction
import com.tangem.tangemtest.extensions.view.beginDelayedTransition
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.PersonalizationItemsManager
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.ItemError
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment
import com.tangem.tangemtest.ucase.variants.personalize.PersonalizationConfigStore
import com.tangem.tangemtest.ucase.variants.personalize.ui.presets.PersonalizationPresetManager
import com.tangem.tangemtest.ucase.variants.personalize.ui.presets.PersonalizationPresetView
import com.tangem.tangemtest.ucase.variants.personalize.ui.presets.RvPresetNamesAdapter
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.PersonalizationItemBuilder
import com.tangem.tangemtest.ucase.variants.responses.ui.ResponseFragment
import ru.dev.gbixahue.eu4d.lib.android._android.views.inflate
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.android.global.threading.post
import ru.dev.gbixahue.eu4d.lib.android.global.threading.postUI
import ru.dev.gbixahue.eu4d.lib.android.global.threading.postWork


/**
[REDACTED_AUTHOR]
 */
class PersonalizationFragment : BaseCardActionFragment(), PersonalizationPresetView {

    override val itemsManager: ItemsManager by lazy { PersonalizationItemsManager(PersonalizationConfigStore(requireContext())) }

    override fun getLayoutId(): Int = R.layout.fg_base_action_layout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
                    contentContainer.beginDelayedTransition(Fade())
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

    override fun handleResponseCardData(card: Card) {
        super.handleResponseCardData(card)
        val bundle = ResponseFragment.setTittle(R.string.fg_name_response_personalization)
        navigateTo(R.id.action_nav_card_action_to_response_screen, bundle, null)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fg_peronalization, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val store = PersonalizationConfigStore(requireContext())
        val presetManager = PersonalizationPresetManager(itemsManager, store, this)
        val result = when (item.itemId) {
            R.id.action_reset -> presetManager.resetToDefault()
            R.id.action_save -> presetManager.savePreset()
            R.id.action_load -> presetManager.loadPreset()
            else -> null
        }
        return if (result == null) super.onOptionsItemSelected(item) else true
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

    override fun showSavePresetDialog(onOk: SafeValueChanged<String>) {
        val dlgController = DialogController()
        val dlg = dlgController.createAlert(requireActivity(), R.layout.dlg_personalization_preset_save)
        dlg.setTitle(R.string.menu_personalization_preset_save)
        dlg.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.btn_cancel)) { dialog, which -> }
        dlg.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.btn_ok)) { dialog, which ->
            val tvName = dlgController.view?.findViewById<EditText>(R.id.et_item)
                    ?: return@setButton
            val name = tvName.text.toString()
            if (name.isEmpty()) showSnackbar("Not saved")
            else onOk.invoke(name)
        }
        dlgController.onShowCallback = {
            dlgController.view?.findViewById<TextView>(R.id.et_item)?.let {
                post(150) {
                    it.requestFocus()
                    val imm = getSystemService(requireContext(), InputMethodManager::class.java)
                    imm?.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
        dlgController.show()
    }

    override fun showLoadPresetDialog(namesList: List<String>, onChoose: SafeValueChanged<String>, onDelete: SafeValueChanged<String>) {
        val dlgController = DialogController()
        dlgController.createAlert(requireActivity(), R.layout.dlg_personalization_preset_load)
                .setTitle(R.string.menu_personalization_preset_load)

        val rvPresetNames: RecyclerView = dlgController.view?.findViewById(R.id.recycler_view) ?: return
        val layoutManager = LinearLayoutManager(context)
        rvPresetNames.layoutManager = layoutManager
        rvPresetNames.addItemDecoration(DividerItemDecoration(activity, layoutManager.orientation))

        val adapter = RvPresetNamesAdapter({
            onChoose(it)
            dlgController.dismiss()
        }, {
            onDelete(it)
            if (rvPresetNames.adapter?.itemCount == 0)
                dlgController.dismiss()
        })
        adapter.setItemList(namesList.toMutableList())

        rvPresetNames.adapter = adapter
        dlgController.show()
    }
}