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
import com.tangem.tangem_sdk_new.DefaultCardManagerDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.TerminalKeysStorage
import com.tangem.tangem_sdk_new.nfc.NfcManager
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.ucase.domain.actions.PersonalizeAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ParamsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.ParamsManagerFactory
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.ui.ActionViewModelFactory
import com.tangem.tangemtest.ucase.ui.ParamsViewModel
import com.tangem.tangemtest.ucase.variants.personalize.dto.CardManagerConfig
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

    private val paramsManager: ParamsManager by lazy { ParamsManagerFactory.createFactory().get(ActionType.Personalize)!! }
    private val paramsVM: ParamsViewModel by viewModels() { ActionViewModelFactory(paramsManager) }

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

        val cardManager = initCardManager() ?: return

        val blockList = mutableListOf<Block>()
        personalizeVM.ldBlockList.observe(viewLifecycleOwner, Observer { list ->
            blockList.clear()
            blockList.addAll(list)
            blockList.forEach { WidgetBuilder().build(it, blockContainer) }
        })
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            personalizeVM.toggleDescriptionVisibility(it)
        })

        paramsVM.setCardManager(cardManager)
        paramsVM.ldResponse.observe(viewLifecycleOwner, Observer {
            Log.d(this, "action response: ${if (it.length > 50) it.substring(0..50) else it}")
            showSnackbarMessage(it)
        })
        paramsVM.seError.observe(viewLifecycleOwner, Observer { showSnackbarMessage(it) })

        fab_action?.setOnClickListener {
            val cardConfig = personalizeVM.createCardConfig(personalizeVM.createConfig(blockList))
            paramsVM.invokeMainAction(mutableMapOf(PersonalizeAction.cardConfig to cardConfig))
        }
    }

    private fun initCardManager(): CardManager? {
        val activity = activity ?: return null
        val nfcManager = NfcManager().apply {
            this.setCurrentActivity(activity)
            activity.lifecycle.addObserver(NfcLifecycleObserver(this))
        }
        val cardManagerDelegate = DefaultCardManagerDelegate(nfcManager.reader).apply {
            this.activity = activity
        }
        return CardManager(nfcManager.reader, cardManagerDelegate, CardManagerConfig.default()).apply {
            this.setTerminalKeysService(TerminalKeysStorage(activity.application))
        }
    }

    protected fun showSnackbarMessage(message: String) {
        Snackbar.make(mainView, message, BaseTransientBottomBar.LENGTH_SHORT).show()
    }

//    private fun getJsonFromAssets(context: Context, fileName: String): String? {
//        return try {
//            val stream: InputStream = context.assets.open(fileName)
//            val size: Int = stream.available()
//            val buffer = ByteArray(size)
//            stream.read(buffer)
//            stream.close()
//            String(buffer, Charset.forName("UTF-8"))
//        } catch (e: IOException) {
//            e.printStackTrace()
//            null
//        }
//    }
}







