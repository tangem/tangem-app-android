package com.tangem.tangemtest.cardUseCase.ui.personalize

import android.content.Context
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
import com.tangem.tangemtest._main.MainViewModel
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.ParamsManager
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.ParamsManagerFactory
import com.tangem.tangemtest.cardUseCase.resources.ActionType
import com.tangem.tangemtest.cardUseCase.ui.card.ActionViewModelFactory
import com.tangem.tangemtest.cardUseCase.ui.card.ParamsViewModel
import com.tangem.tangemtest.cardUseCase.ui.personalize.widgets.WidgetBuilder
import kotlinx.android.synthetic.main.fg_personalize.*
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

/**
* [REDACTED_AUTHOR]
 */
class PersonalizeFragment : Fragment() {

    private val mainActivityVM: MainViewModel by activityViewModels()
    private val personalizeVM: PersonalizeViewModel by viewModels { PersonalizeViewModelFactory(getPersonalizeJson()) }

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

        personalizeVM.ldBlockList.observe(viewLifecycleOwner, Observer { blocList ->
            blocList.forEach { WidgetBuilder().build(it, blockContainer) }
        })
        mainActivityVM.ldDescriptionSwitch.observe(viewLifecycleOwner, Observer {
            personalizeVM.toggleDescriptionVisibility(it)
        })

        paramsVM.setCardManager(CardManager.init(requireActivity()))
        paramsVM.ldResponse.observe(viewLifecycleOwner, Observer {
            Log.d(this, "action response: ${if (it.length > 50) it.substring(0..50) else it}")
            showSnackbarMessage(it)
        })
        paramsVM.seError.observe(viewLifecycleOwner, Observer { showSnackbarMessage(it) })
        fab_action?.setOnClickListener { paramsVM.invokeMainAction() }
    }

    protected fun showSnackbarMessage(message: String) {
        Snackbar.make(mainView, message, BaseTransientBottomBar.LENGTH_SHORT).show()
    }

    private fun getPersonalizeJson(): String {
        return getJsonFromAssets(requireContext(), "personalize_default.json") ?: "[]"
    }

    private fun getJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            val stream: InputStream = context.assets.open(fileName)
            val size: Int = stream.available()
            val buffer = ByteArray(size)
            stream.read(buffer)
            stream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}








