package com.tangem.tangemtest.card_use_cases.ui.card_action

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.tangem.CardManager
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.domain.params_manager.ParamsManager
import com.tangem.tangemtest.card_use_cases.domain.params_manager.ParamsManagerFactory
import com.tangem.tangemtest.card_use_cases.ui.card_action.widgets.ParameterWidget
import com.tangem.tangemtest.card_use_cases.view_models.ActionViewModelFactory
import com.tangem.tangemtest.card_use_cases.view_models.ParamsViewModel
import com.tangem.tangemtest.commons.ActionType
import ru.dev.gbixahue.eu4d.lib.android._android.views.find
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.kotlin.common.LayoutHolder

/**
[REDACTED_AUTHOR]
 */
abstract class BaseCardActionFragment : Fragment(), LayoutHolder {

    protected val incomingParamsContainer: ViewGroup by lazy {
        mainView.findViewById<LinearLayout>(R.id.ll_incoming_params_container)
    }
    protected val viewModel: ParamsViewModel by viewModels { ActionViewModelFactory(paramsManager) }
    protected lateinit var mainView: View

    private val paramsManager: ParamsManager by lazy { ParamsManagerFactory.createFactory().get(getAction())!! }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(this, "onCreateView")
        mainView = inflater.inflate(getLayoutId(), container, false)
        return mainView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(this, "onViewCreated")

        viewModel.setCardManager(CardManager.init(requireActivity()))
        initFab()
        createWidgets { subscribeToViewModelChanges(it) }
    }

    private fun initFab() {
        Log.d(this, "initFab")
        mainView.find<FloatingActionButton>(R.id.fab_action)?.setOnClickListener { viewModel.invokeMainAction() }
    }

    private fun createWidgets(callback: (List<ParameterWidget>) -> Unit) {
        Log.d(this, "createWidgets")
        viewModel.ldParams.observe(viewLifecycleOwner, Observer { paramsList ->
            val widgetList = mutableListOf<ParameterWidget>()
            paramsList.forEach { param ->
                val widget = ParameterWidget(inflateParamView(incomingParamsContainer), param)
                widget.onValueChanged = { tag, value -> viewModel.userChangedParameter(tag, value) }
                widget.onActionBtnClickListener = viewModel.getParameterAction(param.tlvTag)
                widgetList.add(widget)
            }
            callback(widgetList)
        })
    }

    private fun subscribeToViewModelChanges(widgetList: List<ParameterWidget>) {
        Log.d(this, "subscribeToViewModelChanges")

        val tvResponse by lazy { mainView.findViewById<TextView>(R.id.tv_action_response_json) }
        viewModel.ldResponse.observe(viewLifecycleOwner, Observer {
            Log.d(this, "action response: ${if (it.length > 50) it.substring(0..50) else it}")
            tvResponse.text = it
        })
        viewModel.seError.observe(viewLifecycleOwner, Observer { showSnackbarMessage(it) })
        viewModel.seIncomingParameter.observe(viewLifecycleOwner, Observer { param ->
            Log.d(this, "parameter changed from VM - name: ${param.tlvTag.name}, value:${param.data}")
            widgetList.firstOrNull { it.tlvTag == param.tlvTag }?.changeParamValue(param.data)
        })
    }

    protected fun showSnackbarMessage(message: String) {
        Snackbar.make(mainView, message, BaseTransientBottomBar.LENGTH_SHORT).show()
    }

    private fun inflateParamView(where: ViewGroup): ViewGroup {
        val inflater = LayoutInflater.from(where.context)
        val view = inflater.inflate(R.layout.w_card_incoming_param, where, false)
        where.addView(view)
        return view as ViewGroup
    }

    abstract fun getAction(): ActionType
}