package com.tangem.tap.common.extensions

import android.view.View
import android.widget.EditText
import android.widget.TextSwitcher
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout

/**
[REDACTED_AUTHOR]
 */
fun TextView.update(text: String?) {
    if (this.text?.toString() != text) this.text = text
}

fun EditText.update(text: String?) {
    if (this.text?.toString() == text) return

    val textLength = text?.length ?: 0

    //prevent cursor jumping while editing a text
    val cursorPosition = if (selectionEnd > textLength) textLength else selectionEnd
    this.setText(text)
    if (!isFocused || textLength == 0) return

    if (cursorPosition == 0) setSelection(textLength)
    else setSelection(cursorPosition)
}

fun EditText.setOnImeActionListener(action: Int, handler: (EditText) -> Unit) {
    this.setOnEditorActionListener { view, actionId, event ->
        if (actionId == action) {
            handler.invoke(this)
            return@setOnEditorActionListener true
        }
        return@setOnEditorActionListener false
    }
}

fun TextSwitcher.update(text: CharSequence?) {
    val textView = this.currentView as? TextView ?: return

    if (textView.text?.toString() != text?.toString()) this.setText(text)
}

// By default the TextInputLayout didn't activates the error state if the message is empty or null
fun TextInputLayout.enableError(enable: Boolean, errorMessage: String? = null) {
    if (enable) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            error = "Any message"
            if (childCount == 2) getChildAt(1).visibility = View.GONE
        } else {
            error = errorMessage
        }
    } else {
        error = null
        isErrorEnabled = false
    }
}