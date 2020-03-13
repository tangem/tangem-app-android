package com.tangem.tangemtest.card_use_cases.ui.widgets

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.models.params.manager.IncomingParameter
import com.tangem.tangemtest.commons.Action
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
class ParameterWidget(parent: View, private val model: IncomingParameter) {

    private val tvName: TextView = parent.findViewById(R.id.tv_param_name)
    private val etValue: EditText = parent.findViewById(R.id.tv_param_value)
    private val btnAction: Button = parent.findViewById(R.id.btn_action)
    private val valueWatcher: TextWatcher by lazy { getWatcher() }

    val tlvTag: TlvTag = model.tlvTag

    private var parameterActionStorage: (() -> Unit)? = null
    var onParameterAction: (() -> Unit)? = null
        set(value) {
            TransitionManager.beginDelayedTransition(etValue.parent as ViewGroup, AutoTransition())
            if (value != null) {
                parameterActionStorage = value
                setActionButtonTitle()
                btnAction.visibility = View.VISIBLE
            } else {
                btnAction.visibility = View.GONE
            }
            field = value
        }

    var onParameterValueChanged: ((IncomingParameter) -> Unit)? = null

    init {
        tvName.text = model.tlvTag.name
        etValue.addTextChangedListener(valueWatcher)
        btnAction.setOnClickListener { onParameterAction?.invoke() }
    }

    private fun setActionButtonTitle() {
        val action = when (model.tlvTag) {
            TlvTag.CardId -> Action.Scan
            else -> Action.Unknown
        }
        btnAction.setText(action.resName)
    }

    private fun getWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newValue = stringOf(s)
                onParameterAction = if (newValue.isEmpty() && onParameterAction == null) parameterActionStorage
                else null
                model.data = newValue
                onParameterValueChanged?.invoke(model)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    fun changeParamValue(data: Any?, silent: Boolean = true) {
        Log.d(this, "changeParamValue: tag: $tlvTag, value: $data")
        if (silent) {
            etValue.removeTextChangedListener(valueWatcher)
            etValue.setText(stringOf(data))
            etValue.addTextChangedListener(valueWatcher)
        } else {
            etValue.setText(stringOf(data))
        }
        onParameterAction = null
    }
}