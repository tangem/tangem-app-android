package com.tangem.features.polymarket.impl.main.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_at_sign_20
import com.tangem.core.ui.res.generated.icons.ic_basketball_20
import com.tangem.core.ui.res.generated.icons.ic_basketball_turned_20
import com.tangem.core.ui.res.generated.icons.ic_car_sports_20
import com.tangem.core.ui.res.generated.icons.ic_chart_ascending_20
import com.tangem.core.ui.res.generated.icons.ic_coin_token_20
import com.tangem.core.ui.res.generated.icons.ic_document_pen_20
import com.tangem.core.ui.res.generated.icons.ic_earth_20
import com.tangem.core.ui.res.generated.icons.ic_football_20
import com.tangem.core.ui.res.generated.icons.ic_gamepad_20
import com.tangem.core.ui.res.generated.icons.ic_globe_arrow_20
import com.tangem.core.ui.res.generated.icons.ic_government_20
import com.tangem.core.ui.res.generated.icons.ic_hockey_20
import com.tangem.core.ui.res.generated.icons.ic_monitor_20
import com.tangem.core.ui.res.generated.icons.ic_new_20
import com.tangem.core.ui.res.generated.icons.ic_pie_chart_20
import com.tangem.core.ui.res.generated.icons.ic_rugby_ball_20
import com.tangem.core.ui.res.generated.icons.ic_sack_20
import com.tangem.core.ui.res.generated.icons.ic_tennis_20
import com.tangem.core.ui.res.generated.icons.ic_theater_20
import com.tangem.core.ui.res.generated.icons.ic_weather_20
import com.tangem.domain.polymarket.model.PolymarketCategoryIcon

internal val PolymarketCategoryIcon.tabIcon: ImageVector
    get() = when (this) {
        PolymarketCategoryIcon.WORLD -> Icons.ic_earth_20
        PolymarketCategoryIcon.GEOPOLITICS -> Icons.ic_globe_arrow_20
        PolymarketCategoryIcon.ELECTIONS -> Icons.ic_document_pen_20
        PolymarketCategoryIcon.SOCCER -> Icons.ic_football_20
        PolymarketCategoryIcon.NBA -> Icons.ic_basketball_turned_20
        PolymarketCategoryIcon.NFL -> Icons.ic_rugby_ball_20
        PolymarketCategoryIcon.HOCKEY -> Icons.ic_hockey_20
        PolymarketCategoryIcon.TENNIS -> Icons.ic_tennis_20
        PolymarketCategoryIcon.FORMULA_1 -> Icons.ic_car_sports_20
        PolymarketCategoryIcon.TECH -> Icons.ic_monitor_20
        PolymarketCategoryIcon.ECONOMY -> Icons.ic_pie_chart_20
        PolymarketCategoryIcon.MENTIONS -> Icons.ic_at_sign_20
        PolymarketCategoryIcon.TRENDING -> Icons.ic_chart_ascending_20
        PolymarketCategoryIcon.NEW -> Icons.ic_new_20
        PolymarketCategoryIcon.POLITICS -> Icons.ic_government_20
        PolymarketCategoryIcon.SPORTS -> Icons.ic_basketball_20
        PolymarketCategoryIcon.ESPORTS -> Icons.ic_gamepad_20
        PolymarketCategoryIcon.CULTURE -> Icons.ic_theater_20
        PolymarketCategoryIcon.WEATHER -> Icons.ic_weather_20
        PolymarketCategoryIcon.FINANCE -> Icons.ic_sack_20
        PolymarketCategoryIcon.CRYPTO -> Icons.ic_coin_token_20
    }