package com.tangem.common.ui.tokens

import com.tangem.core.ui.extensions.TextReference

/**
 * The earn badge of a token title, ready to show: the host decides which rate it carries.
 *
 * @property isActive whether the user already earns on the token
 */
data class EarnBadge(val text: TextReference, val isActive: Boolean)