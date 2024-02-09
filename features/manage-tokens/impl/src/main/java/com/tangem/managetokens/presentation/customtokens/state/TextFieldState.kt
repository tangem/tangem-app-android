package com.tangem.managetokens.presentation.customtokens.state

internal sealed class TextFieldState {
    object Loading : TextFieldState()

    data class Editable(
        val value: String,
        val isEnabled: Boolean,
        val error: AddCustomTokenWarning? = null,
        val onValueChange: (String) -> Unit,
        val onFocusExit: () -> Unit,
    ) : TextFieldState()

    fun isInputValid(): Boolean = this is Editable && value.isNotBlank() && error == null

    fun copySealed(
        value: String = (this as? Editable)?.value ?: "",
        isEnabled: Boolean = (this as? Editable)?.isEnabled ?: true,
        error: AddCustomTokenWarning? = (this as? Editable)?.error,
        onValueChange: (String) -> Unit = (this as? Editable)?.onValueChange ?: {},
    ): TextFieldState {
        return when (this) {
            is Editable -> this.copy(
                value = value,
                isEnabled = isEnabled,
                error = error,
                onValueChange = onValueChange,
            )
            is Loading -> this
        }
    }
}