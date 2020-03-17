package com.tangem.tangemtest.card_use_cases.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.ui.widgets.ParameterWidget
import com.tangem.tangemtest.card_use_cases.view_models.ParamsViewModel
import com.tangem.tangemtest.commons.Action
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.kotlin.common.LayoutHolder

/**
[REDACTED_AUTHOR]
 */
abstract class BaseCardActionFragment : Fragment(), LayoutHolder {

    protected lateinit var mainView: View
    protected val incomingParamsContainer: ViewGroup by lazy {
        mainView.findViewById<LinearLayout>(R.id.ll_incoming_params_container)
    }

    protected val viewModel: ParamsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(this, "onCreateView")
        mainView = inflater.inflate(getLayoutId(), container, false)
        return mainView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(this, "onViewCreated")
        val activity = activity ?: return

        try {
            viewModel.init(activity, getAction())
            initFab()
            val widgetList = createWidgets()
            subscribeToViewModelChanges(widgetList)
        } catch (e: Exception) {
            Log.e(this, e)
            showSnackbarMessage(e.toString())
        }
    }

    private fun initFab() {
        Log.d(this, "initFab")
        val fab = mainView.findViewById<ExtendedFloatingActionButton>(R.id.fab_action) ?: return

        fab.setText(getAction().resName)
        fab.setOnClickListener { viewModel.invokeMainAction() }
    }

    private fun createWidgets(): MutableList<ParameterWidget> {
        Log.d(this, "createWidgets")

        val widgetList = mutableListOf<ParameterWidget>()
        viewModel.getInitialParams()?.forEach { param ->
            val widget = ParameterWidget(inflateParamView(incomingParamsContainer), param)
            widget.onParameterValueChanged = { viewModel.userChangedParameter(it) }
            widget.onParameterAction = viewModel.getParameterAction(param.tlvTag)
            widgetList.add(widget)
        }
        return widgetList
    }

    private fun subscribeToViewModelChanges(widgetList: MutableList<ParameterWidget>) {
        Log.d(this, "subscribeToViewModelChanges")

        viewModel.ldResponse.observe(viewLifecycleOwner, Observer {
            Log.d(this, "action response: ${if (it.length > 50) it.substring(0..50) else it}")
            mainView.findViewById<TextView>(R.id.tv_action_response_json).text = it
        })
        viewModel.ldError.observe(viewLifecycleOwner, Observer { showSnackbarMessage(it) })
        viewModel.ldIncomingParameter.observe(viewLifecycleOwner, Observer { param ->
            Log.d(this, "parameter changed from VM - name: ${param.tlvTag.name}, value:${param.data}")
            widgetList.firstOrNull { it.tlvTag == param.tlvTag }?.changeParamValue(param.data)
        })
    }

    protected fun showSnackbarMessage(message: String) {
        Snackbar.make(mainView, message, BaseTransientBottomBar.LENGTH_SHORT).show()
    }

    private fun inflateParamView(where: ViewGroup): View {
        val inflater = LayoutInflater.from(where.context)
        val view = inflater.inflate(R.layout.w_card_incoming_param, where, false)
        where.addView(view)
        inflater.inflate(R.layout.m_divider_h, where, true)
        return view
    }

    abstract fun getAction(): Action
}