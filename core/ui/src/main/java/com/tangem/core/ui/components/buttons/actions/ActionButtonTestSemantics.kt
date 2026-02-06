package com.tangem.core.ui.components.buttons.actions

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

val IsDimmedKey = SemanticsPropertyKey<Boolean>("IsDimmed")
val HasBadgeKey = SemanticsPropertyKey<Boolean>("HasBadge")

var SemanticsPropertyReceiver.isDimmed by IsDimmedKey
var SemanticsPropertyReceiver.hasBadge by HasBadgeKey