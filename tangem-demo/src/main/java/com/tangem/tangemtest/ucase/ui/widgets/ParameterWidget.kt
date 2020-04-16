package com.tangem.tangemtest.ucase.ui.widgets

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.impl.EditTextItem
import com.tangem.tangemtest.extensions.view.beginDelayedTransition
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.resources.MainResourceHolder
import com.tangem.tangemtest.ucase.resources.Resources
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
class ParameterWidget(
        private val parent: ViewGroup,
        item: Item
) {

    val id: Id = item.id
    val typedItem = item as EditTextItem

    var onValueChanged: ((Id, Any?) -> Unit)? = null
    var onActionBtnClickListener: (() -> Unit)? = null
        set(value) {
            field = value
            toggleActionBtnVisibility()
        }

    private val tilValue: TextInputLayout = parent.findViewById(R.id.til_param)
    private val etValue: TextInputEditText = parent.findViewById(R.id.et_param)
    private val btnAction: Button = parent.findViewById(R.id.btn_action)
    private val valueWatcher: TextWatcher by lazy { getWatcher() }

    private val descriptionContainer: ViewGroup by lazy { parent.findViewById<ViewGroup>(R.id.container_description) }
    private val tvDescription: TextView? by lazy { descriptionContainer.findViewById<TextView>(R.id.tv_description) }

    private var actionBtnVisibilityState: Int = btnAction.visibility
    private var value: Any? = typedItem.viewModel.data

    init {
        tilValue.hint = tilValue.context.getString(getResNameId())
        etValue.setText(stringOf(typedItem.viewModel.data))
        etValue.addTextChangedListener(valueWatcher)
        btnAction.setOnClickListener { onActionBtnClickListener?.invoke() }
        btnAction.setText(getResNameId(ActionType.Scan))
        toggleActionBtnVisibility()
        initDescriptionWidget()
    }

    fun changeParamValue(data: Any?, silent: Boolean = true) {
        Log.d(this, "changeParamValue: tag: $id, value: $data")
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

    fun toggleDescriptionVisibility(state: Boolean) {
        (parent.parent as ViewGroup).beginDelayedTransition()
        descriptionContainer.visibility = if (state) View.VISIBLE else View.GONE
    }

    private fun getWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                Log.d(this, "afterTextChanged $editable")
                value = if (editable.isNullOrEmpty()) null else editable.toString()
                toggleActionBtnVisibility()
                onValueChanged?.invoke(id, value)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    private fun toggleActionBtnVisibility() {
        fun switchVisibilityState(newState: Int) {
            actionBtnVisibilityState = newState
            parent.beginDelayedTransition()
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

    private fun initDescriptionWidget() {
        getResDescription()?.let { tvDescription?.setText(it) }
    }
}

fun ParameterWidget.getResNameId(id: Id? = null): Int = MainResourceHolder.safeGet<Resources>(id
        ?: this.id).resName

fun ParameterWidget.getResDescription(): Int? = MainResourceHolder.safeGet<Resources>(id).resDescription