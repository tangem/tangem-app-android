package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.utils.transformer.Transformer

/** `null` clears the icon: hot wallets have no wallet-interaction icon on the button */
internal class UpdateButtonIconTransformer(
    private val icon: TangemIconUM?,
) : Transformer<JointAccountDisplayNameUM> {

    override fun transform(prevState: JointAccountDisplayNameUM): JointAccountDisplayNameUM =
        prevState.copy(buttonIcon = icon)
}