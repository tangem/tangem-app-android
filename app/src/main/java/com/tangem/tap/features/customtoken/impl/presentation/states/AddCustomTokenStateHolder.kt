package com.tangem.tap.features.customtoken.impl.presentation.states

import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenChooseTokenBottomSheet
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenFloatingButton
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenForm
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenTestBlock
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenWarning
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokensToolbar

/**
 * State holder of add custom token screen
 *
[REDACTED_AUTHOR]
 */
internal sealed interface AddCustomTokenStateHolder {

    /** Lambda be invoked when system back action is been called */
    val onBackButtonClick: () -> Unit

    /** Toolbar model */
    val toolbar: AddCustomTokensToolbar

    /** Form model */
    val form: AddCustomTokenForm

    /** Warnings */
    val warnings: Set<AddCustomTokenWarning>

    /** Floating button model */
    val floatingButton: AddCustomTokenFloatingButton

    /**
     * Util function that allow to make a copy
     *
     * @param onBackButtonClick lambda be invoked when system back action is been called
     * @param toolbar           toolbar model
     * @param form              form model
     * @param warnings          warnings
     * @param floatingButton    floating button model
     */
    fun copySealed(
        onBackButtonClick: () -> Unit = this.onBackButtonClick,
        toolbar: AddCustomTokensToolbar = this.toolbar,
        form: AddCustomTokenForm = this.form,
        warnings: Set<AddCustomTokenWarning> = this.warnings,
        floatingButton: AddCustomTokenFloatingButton = this.floatingButton,
    ): AddCustomTokenStateHolder {
        return when (this) {
            is Content -> copy(onBackButtonClick, toolbar, form, warnings, floatingButton)
            is TestContent -> copy(onBackButtonClick, toolbar, form, warnings, floatingButton)
        }
    }

    /**
     * Content state
     *
     * @property onBackButtonClick lambda be invoked when system back action is been called
     * @property toolbar           toolbar model
     * @property form              form model
     * @property warnings          warnings
     * @property floatingButton    floating button model
     */
    data class Content(
        override val onBackButtonClick: () -> Unit,
        override val toolbar: AddCustomTokensToolbar,
        override val form: AddCustomTokenForm,
        override val warnings: Set<AddCustomTokenWarning>,
        override val floatingButton: AddCustomTokenFloatingButton,
    ) : AddCustomTokenStateHolder

    /**
     * Content state with fields for testing
     *
     * @property onBackButtonClick lambda be invoked when system back action is been called
     * @property toolbar           toolbar model
     * @property form              form model
     * @property warnings          warnings
     * @property floatingButton    floating button model
     * @property testBlock         test block model
     * @property bottomSheet       bottom sheet model
     */
    data class TestContent(
        override val onBackButtonClick: () -> Unit,
        override val toolbar: AddCustomTokensToolbar,
        override val form: AddCustomTokenForm,
        override val warnings: Set<AddCustomTokenWarning>,
        override val floatingButton: AddCustomTokenFloatingButton,
        val testBlock: AddCustomTokenTestBlock,
        val bottomSheet: AddCustomTokenChooseTokenBottomSheet,
    ) : AddCustomTokenStateHolder
}