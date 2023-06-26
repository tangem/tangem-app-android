package com.tangem.tap.features.send.ui

import android.R
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText

class EditTextCustomPaste : TextInputEditText {

    private var onSystemPasteButtonClickListener: (() -> Unit)? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setOnSystemPasteButtonClickListener(callback: () -> Unit) {
        onSystemPasteButtonClickListener = callback
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        val isConsumed = super.onTextContextMenuItem(id)

        if (id == R.id.paste) onSystemPasteButtonClickListener?.invoke()

        return isConsumed
    }
}