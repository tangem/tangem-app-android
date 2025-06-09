package com.tangem.core.ui.components.provider.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.badge.entity.BadgeUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * Provider Row - Choose Crypto component UI Model
 *
 * @property title      title text
 * @property subtitle   subtitle text
 * @property infoText   info text
 * @property iconUrl    icon url
 * @property isSelected indicated whether component is selected (border outline)
 * @property badgeList  list of badge components
 * @property labelUM    label component (whether AuditLabel or text)
 */
data class ProviderChooseUM(
    val title: TextReference,
    val subtitle: TextReference,
    val infoText: TextReference,
    val iconUrl: String,
    val isSelected: Boolean,
    val badgeList: ImmutableList<BadgeUM>,
    val labelUM: LabelUM?,
) {
    @Immutable
    sealed class LabelUM {
        data class Info(
            val auditLabelUM: AuditLabelUM,
        ) : LabelUM()

        data class Text(
            val text: TextReference,
        ) : LabelUM()
    }
}