package com.tangem.tangemtest.card_use_cases.ui.personalize

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.ui.personalize.view_model.PersonalizeViewModel
import com.tangem.tangemtest.card_use_cases.ui.personalize.view_model.PersonalizeViewModelFactory
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.WidgetBuilder
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

/**
[REDACTED_AUTHOR]
 */
class PersonalizeFragment : Fragment() {

    protected val blockContainer: ViewGroup by lazy {
        mainView.findViewById<LinearLayout>(R.id.ll_container)
    }
    protected val viewModel: PersonalizeViewModel by viewModels { PersonalizeViewModelFactory(getPersonalizeJson()) }
    protected lateinit var mainView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(this, "onCreateView")
        mainView = inflater.inflate(R.layout.fg_personalize, container, false)
        return mainView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.ldBlockList.observe(viewLifecycleOwner, Observer { blocList ->
            blocList.forEach { WidgetBuilder().build(it, blockContainer) }

        })
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







