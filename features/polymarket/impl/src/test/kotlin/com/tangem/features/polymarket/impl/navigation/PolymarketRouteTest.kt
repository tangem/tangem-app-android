package com.tangem.features.polymarket.impl.navigation

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketRouteTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN route WHEN userWalletIdOrNull THEN the wallet it carries is returned`(model: RouteModel) {
        // Act
        val actual = model.route.userWalletIdOrNull

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        RouteModel(PolymarketRoute.Entry, expected = null),
        RouteModel(PolymarketRoute.Onboarding(USER_WALLET_ID)),
        RouteModel(PolymarketRoute.Main(USER_WALLET_ID)),
        RouteModel(PolymarketRoute.History(USER_WALLET_ID)),
        RouteModel(PolymarketRoute.FastTradingSettings(USER_WALLET_ID)),
        RouteModel(PolymarketRoute.Positions(USER_WALLET_ID, PolymarketPositionsTab.ACTIVE)),
        RouteModel(PolymarketRoute.PositionDetails(USER_WALLET_ID, POSITION_ID)),
        RouteModel(PolymarketRoute.PositionResult(USER_WALLET_ID, mockk<PolymarketPosition.Open>())),
        RouteModel(PolymarketRoute.EventDetails(eventId = "event", userWalletId = USER_WALLET_ID)),
        RouteModel(PolymarketRoute.PlacePrediction(USER_WALLET_ID, eventId = "event", marketId = "m", assetId = "a")),
        RouteModel(PolymarketRoute.ClosePosition(USER_WALLET_ID, POSITION_ID, marketId = "m")),
        RouteModel(PolymarketRoute.Search(USER_WALLET_ID)),
    )

    data class RouteModel(val route: PolymarketRoute, val expected: UserWalletId? = USER_WALLET_ID)

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val POSITION_ID = PolymarketPositionId(conditionId = "condition", assetId = "asset")
    }
}