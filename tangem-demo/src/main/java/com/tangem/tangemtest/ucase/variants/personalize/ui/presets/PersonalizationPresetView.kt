package com.tangem.tangemtest.ucase.variants.personalize.ui.presets

import com.tangem.tangemtest._arch.structure.abstraction.SafeValueChanged
import com.tangem.tangemtest.ucase.tunnel.SnackbarHolder

interface PersonalizationPresetView : SnackbarHolder {
    fun showSavePresetDialog(onOk: SafeValueChanged<String>)
    fun showLoadPresetDialog(namesList: List<String>, onChoose: SafeValueChanged<String>, onDelete: SafeValueChanged<String>)
}