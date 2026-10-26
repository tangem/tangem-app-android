package com.tangem.domain.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetActionRequirementAmountUseCaseTest {

    private val useCase = GetActionRequirementAmountUseCase()

    @Test
    fun `GIVEN polygon CLAIM_REWARDS WHEN invoke THEN one`() {
        // Act
        val actual = useCase(StakingIntegrationID.StakeKit.EthereumToken.Polygon.value, StakingActionType.CLAIM_REWARDS)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal.ONE)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN any other id and action pair WHEN invoke THEN null`(model: RequirementModel) {
        // Act
        val actual = useCase(model.integrationId, model.actionType)

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels(): List<RequirementModel> {
        val polygon = StakingIntegrationID.StakeKit.EthereumToken.Polygon
        return StakingActionType.entries
            .filter { it != StakingActionType.CLAIM_REWARDS }
            .map { RequirementModel(polygon.value, it) } +
            StakingIntegrationID.entries
                .filter { it != polygon }
                .map { RequirementModel(it.value, StakingActionType.CLAIM_REWARDS) } +
            RequirementModel("unknown-id", StakingActionType.CLAIM_REWARDS)
    }

    internal data class RequirementModel(val integrationId: String, val actionType: StakingActionType)
}