package com.tangem.screens.tangempay

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import com.tangem.core.ui.test.MoneyAccountTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.kakao.common.utilities.getResourceString
import kotlin.math.abs
import com.tangem.core.res.R as CoreResR

internal fun SemanticsNodeInteractionsProvider.expandTangemPayGroupIfCollapsed() {
    runCatching {
        val groupRows = hasTestTag(TangemPayTestTags.MAIN_SCREEN_TILE) or hasTestTag(MoneyAccountTestTags.MAIN_ROW)
        val rows = onAllNodes(groupRows, useUnmergedTree = true).fetchSemanticsNodes(false)
        val groupTitle = getResourceString(CoreResR.string.tangempay_main_group_title)
        val headers = onAllNodes(hasText(groupTitle) and hasClickAction())
        headers.fetchSemanticsNodes(false).forEachIndexed { index, header ->
            if (rows.none { row -> row.isOnSamePageAs(header) }) {
                headers[index].performSemanticsAction(SemanticsActions.OnClick)
            }
        }
    }
}

private fun SemanticsNode.isOnSamePageAs(other: SemanticsNode): Boolean =
    abs(positionInRoot.x - other.positionInRoot.x) < other.size.width