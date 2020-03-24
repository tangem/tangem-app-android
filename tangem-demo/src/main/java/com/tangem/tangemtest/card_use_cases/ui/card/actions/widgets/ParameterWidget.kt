package com.tangem.tangemtest.card_use_cases.ui.card.actions.widgets

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.domain.params_manager.IncomingParameter
import com.tangem.tangemtest.commons.ActionType
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
class ParameterWidget(private val parent: ViewGroup, model: IncomingParameter) {

    val tlvTag: TlvTag = model.tlvTag

    var onValueChanged: ((TlvTag, Any?) -> Unit)? = null
    var onActionBtnClickListener: (() -> Unit)? = null
        set(value) {
            field = value
            toggleActionBtnVisibility()
        }

    private val tilValue: TextInputLayout = parent.findViewById(R.id.til_param)
    private val etValue: TextInputEditText = parent.findViewById(R.id.et_param)
    private val btnAction: Button = parent.findViewById(R.id.btn_action)
    private val valueWatcher: TextWatcher by lazy { getWatcher() }

    private var actionBtnVisibilityState: Int = btnAction.visibility
    private var value: Any? = model.data

    init {
        tilValue.hint = model.tlvTag.name
        etValue.setText(stringOf(model.data))
        etValue.addTextChangedListener(valueWatcher)
        btnAction.setOnClickListener { onActionBtnClickListener?.invoke() }
        btnAction.text = btnAction.context.getString(getActionResName())
        toggleActionBtnVisibility()
    }

    fun changeParamValue(data: Any?, silent: Boolean = true) {
        Log.d(this, "changeParamValue: tag: $tlvTag, value: $data")
        value = data
        toggleActionBtnVisibility()
        if (silent) {
            etValue.removeTextChangedListener(valueWatcher)
            etValue.setText(stringOf(value))
            etValue.addTextChangedListener(valueWatcher)
        } else {
            etValue.setText(stringOf(value))
        }
    }

    private fun getWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                Log.d(this, "afterTextChanged $editable")
                value = if (editable.isNullOrEmpty()) null else editable.toString()
                toggleActionBtnVisibility()
                onValueChanged?.invoke(tlvTag, value)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    private fun getActionResName(): Int {
        val action = when (tlvTag) {
            TlvTag.CardId -> ActionType.Scan
            else -> ActionType.Unknown
        }
        return action.resName
    }

    private fun toggleActionBtnVisibility() {
        fun switchVisibilityState(newState: Int) {
            actionBtnVisibilityState = newState
            TransitionManager.beginDelayedTransition(parent, AutoTransition())
            btnAction.visibility = actionBtnVisibilityState
        }
        when {
            onActionBtnClickListener == null -> {
                if (actionBtnVisibilityState == View.GONE) return
                switchVisibilityState(View.GONE)
            }
            value == null && actionBtnVisibilityState != View.VISIBLE -> switchVisibilityState(View.VISIBLE)
            value != null && actionBtnVisibilityState != View.GONE -> switchVisibilityState(View.GONE)
        }
    }
}