package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.tangem.TangemSdk
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.ParamsManagerFactory
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.CardError
import com.tangem.tangemtest.ucase.tunnel.ItemError
import com.tangem.tangemtest.ucase.ui.ActionViewModelFactory
import com.tangem.tangemtest.ucase.ui.ParamsViewModel
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.WidgetBuilder
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class PersonalizeFragment : Fragment(), ActionView {

    private val mainActivityVM: MainViewModel by activityViewModels()
    private val personalizeVM: PersonalizeViewModel by viewModels { PersonalizeViewModelFactory(PersonalizeConfig()) }

    private val itemsManager: ItemsManager by lazy { ParamsManagerFactory.createFactory().get(ActionType.Personalize)!! }
    private val paramsVM: ParamsViewModel by viewModels() { ActionViewModelFactory(itemsManager) }

    private lateinit var mainView: View
    private val blockContainer: ViewGroup by lazy { mainView.findViewById<LinearLayout>(R.id.ll_container) }
    private val actionFab: FloatingActionButton by lazy { mainView.findViewById<FloatingActionButton>(R.id.fab_action) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(this, "onCreateView")
        mainView = inflater.inflate(R.layout.fg_personalize, container, false)
        return mainView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val blockList = mutableListOf<Item>()
        personalizeVM.ldBlockList.observe(viewLifecycleOwner, Observer { list ->
            blockList.clear()
            blockList.addAll(list)
            blockList.forEach { WidgetBuilder().build(it, blockContainer) }
            paramsVM.attachToPayload(mutableMapOf(
                    PayloadKey.actionView to this as ActionView,
                    PayloadKey.itemList to blockList
            ))

        })
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            personalizeVM.toggleDescriptionVisibility(it)
        })

        val cardManager = TangemSdk.init(requireActivity())
        paramsVM.setCardManager(cardManager)
        paramsVM.ldResponse.observe(viewLifecycleOwner, Observer {
            Log.d(this, "action response: ${if (it.length > 50) it.substring(0..50) else it}")
            showSnackbarMessage(it)
        })
        paramsVM.seError.observe(viewLifecycleOwner, Observer { showSnackbarMessage(it) })

        actionFab.setOnClickListener { paramsVM.invokeMainAction() }
    }

    protected fun showSnackbarMessage(message: String) {
        Snackbar.make(mainView, message, BaseTransientBottomBar.LENGTH_SHORT).show()
    }

    override fun showActionFab(show: Boolean) {
        if (show) actionFab.show() else actionFab.hide()
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

    override fun showSnackbar(id: Int) {
        showSnackbar(requireContext().getString(id))
    }

    override fun showSnackbar(message: String) {
        Snackbar.make(mainView, message, BaseTransientBottomBar.LENGTH_SHORT).show()
    }
}







