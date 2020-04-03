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
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.tangem.CardManager
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.ParamsManagerFactory
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.ui.ActionViewModelFactory
import com.tangem.tangemtest.ucase.ui.ParamsViewModel
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.WidgetBuilder
import kotlinx.android.synthetic.main.fg_personalize.*
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class PersonalizeFragment : Fragment() {

    private val mainActivityVM: MainViewModel by activityViewModels()
    private val personalizeVM: PersonalizeViewModel by viewModels { PersonalizeViewModelFactory(PersonalizeConfig()) }

    private val itemsManager: ItemsManager by lazy { ParamsManagerFactory.createFactory().get(ActionType.Personalize)!! }
    private val paramsVM: ParamsViewModel by viewModels() { ActionViewModelFactory(itemsManager) }

    private val blockContainer: ViewGroup by lazy {
        mainView.findViewById<LinearLayout>(R.id.ll_container)
    }
    private lateinit var mainView: View

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
        })
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            personalizeVM.toggleDescriptionVisibility(it)
        })

        val cardManager = CardManager.init(requireActivity())
        paramsVM.setCardManager(cardManager)
        paramsVM.ldResponse.observe(viewLifecycleOwner, Observer {
            Log.d(this, "action response: ${if (it.length > 50) it.substring(0..50) else it}")
            showSnackbarMessage(it)
        })
        paramsVM.seError.observe(viewLifecycleOwner, Observer { showSnackbarMessage(it) })

        fab_action?.setOnClickListener {
            val cardConfig = personalizeVM.createCardConfig(personalizeVM.createConfig(blockList))
            paramsVM.attachToPayload(mutableMapOf(PayloadKey.cardConfig to cardConfig))
            paramsVM.invokeMainAction()
        }
    }

    protected fun showSnackbarMessage(message: String) {
        Snackbar.make(mainView, message, BaseTransientBottomBar.LENGTH_SHORT).show()
    }
}







