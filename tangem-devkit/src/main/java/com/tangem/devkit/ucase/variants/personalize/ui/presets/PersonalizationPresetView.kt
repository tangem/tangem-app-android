package com.tangem.devkit.ucase.variants.personalize.ui.presets

import com.tangem.devkit._arch.structure.abstraction.SafeValueChanged
import com.tangem.devkit.ucase.tunnel.SnackbarHolder

interface PersonalizationPresetView : SnackbarHolder {
    fun showSavePresetDialog(onOk: SafeValueChanged<String>)
    fun showLoadPresetDialog(namesList: List<String>, onChoose: SafeValueChanged<String>, onDelete: SafeValueChanged<String>)
}