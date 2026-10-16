package com.tangem.features.polymarket.impl.history.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.history.ui.PolymarketActivityDetailsSheet
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityDetailsUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityDetailsUM.Row
import kotlinx.collections.immutable.persistentListOf

@Preview(name = "Light", widthDp = 360, heightDp = 800)
@Preview(name = "Dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun PolymarketActivityDetailsPreview(
    @PreviewParameter(ActivityDetailsPreviewProvider::class) state: PolymarketActivityDetailsUM,
) {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = LocalInspectionMode.current) {
        PolymarketActivityDetailsSheet(state = state, onDismiss = {})
    }
}

internal class ActivityDetailsPreviewProvider : CollectionPreviewParameterProvider<PolymarketActivityDetailsUM>(
    activityDetailsPreviews(),
)

private fun activityDetailsPreviews(): List<PolymarketActivityDetailsUM> {
    val bought = PolymarketActivityDetailsUM(
        title = resourceReference(R.string.prediction_history_bought),
        date = stringReference("Jul 9, 2026, 14:32"),
        headline = null,
        amount = stringReference("-$166.84"),
        marketTitle = stringReference("Will Uzbekistan win the 2026 FIFA World Cup?"),
        iconUrl = null,
        outcome = stringReference("No"),
        outcomePrice = stringReference("67¢"),
        outcomeStatus = TangemBadge.Status.Error,
        rows = persistentListOf(
            Row(resourceReference(R.string.prediction_positions_quantity), stringReference("248.50")),
            Row(stringReference("Price"), stringReference("$0.67")),
            Row(stringReference("Fees"), stringReference("$0.55")),
            Row(stringReference("Notional Gross"), stringReference("$166.50")),
        ),
        onGoToPrediction = {},
    )
    val won = bought.copy(
        title = resourceReference(R.string.prediction_history_redeemed),
        headline = stringReference("Your Prediction Won"),
        amount = stringReference("$240.84"),
        rows = persistentListOf(),
    )
    return listOf(
        bought,
        bought.copy(title = resourceReference(R.string.prediction_history_sold), amount = stringReference("$166.84")),
        won,
        won.copy(
            title = stringReference("Loss"),
            headline = stringReference("Your Prediction Loss"),
            amount = null,
        ),
        won.copy(headline = stringReference("Your Prediction Split")),
        bought.copy(
            outcome = stringReference("Aurora Gaming"),
            outcomeStatus = TangemBadge.Status.Error,
            marketTitle = stringReference("Counter-Strike: 1WIN vs Aurora Gaming (BO3) - ESL Pro League Playoffs"),
        ),
    )
}