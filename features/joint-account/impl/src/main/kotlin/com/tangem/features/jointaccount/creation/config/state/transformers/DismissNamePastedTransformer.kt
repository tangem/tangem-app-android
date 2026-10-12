package com.tangem.features.jointaccount.creation.config.state.transformers

import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.utils.transformer.Transformer

/** Dismisses the cursor-to-end indication once [SimpleTextField][com.tangem.core.ui.components.fields.SimpleTextField] has consumed it */
internal object DismissNamePastedTransformer : Transformer<JointAccountConfigUM> {

    override fun transform(prevState: JointAccountConfigUM): JointAccountConfigUM {
        return prevState.copy(isValuePasted = false)
    }
}