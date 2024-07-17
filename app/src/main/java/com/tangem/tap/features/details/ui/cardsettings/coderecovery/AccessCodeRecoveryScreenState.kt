package com.tangem.tap.features.details.ui.cardsettings.coderecovery

/**
 * @property enabledOnCard Indicates whether access code recovery is enabled on the card
 * @property enabledSelection Represents the currently selected option in the app (not yet saved on the card)
 * @property isSaveChangesEnabled Determines if the user is allowed to save their selection to the card
 * @property onSaveChangesClick Callback function called when the user wants to apply the selected option
 * @property onOptionClick Callback function called when the user selects an option
 * */
data class AccessCodeRecoveryScreenState(
    val enabledOnCard: Boolean,
    val enabledSelection: Boolean,
    val isSaveChangesEnabled: Boolean,
    val onSaveChangesClick: () -> Unit,
    val onOptionClick: (Boolean) -> Unit,
)
