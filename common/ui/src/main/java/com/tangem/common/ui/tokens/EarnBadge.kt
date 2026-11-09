package com.tangem.common.ui.tokens

import com.tangem.core.ui.extensions.TextReference

/** [isActive] is true when the user already earns on the token. */
data class EarnBadge(val text: TextReference, val isActive: Boolean)