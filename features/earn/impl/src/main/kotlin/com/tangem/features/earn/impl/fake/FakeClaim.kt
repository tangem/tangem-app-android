package com.tangem.features.earn.impl.fake

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.product.api.session.EarnActionInput
import com.tangem.features.earn.product.api.session.EarnFlowAction

internal object FakeClaim : EarnFlowAction {

    override val id: String = "claim"

    override val label: TextReference = stringReference("Claim")

    override val input: EarnActionInput = EarnActionInput.None

    override val isGaslessAllowed: Boolean = false

    override fun title(productTitle: TextReference): TextReference = label
}