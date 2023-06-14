package com.tangem.tap.features.send.ui

import android.R
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText

class EditTextCustomPaste @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextInputEditText(context, attrs, defStyleAttr) {

    private var onSystemPasteButtonClickListener: (() -> Unit)? = null

    fun setOnSystemPasteButtonClickListener(callback: () -> Unit) {
        onSystemPasteButtonClickListener = callback
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        val isConsumed = super.onTextContextMenuItem(id)

        if (id == R.id.paste) onSystemPasteButtonClickListener?.invoke()

        return isConsumed
    }
}