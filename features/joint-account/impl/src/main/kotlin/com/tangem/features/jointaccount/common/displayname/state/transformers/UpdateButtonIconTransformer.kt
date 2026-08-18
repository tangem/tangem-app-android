package com.tangem.features.jointaccount.common.displayname.state.transformers

import androidx.annotation.DrawableRes
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.utils.transformer.Transformer

/** `null` clears the icon — hot wallets have no wallet-interaction icon on the button */
internal class UpdateButtonIconTransformer(
    @DrawableRes private val iconRes: Int?,
) : Transformer<JointAccountDisplayNameUM> {

    override fun transform(prevState: JointAccountDisplayNameUM): JointAccountDisplayNameUM =
        prevState.copy(buttonIconRes = iconRes)
}