package com.tangem.data.jointaccount.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.jointaccount.FIXTURE_ALICE_ADDRESS
import com.tangem.data.jointaccount.createJointAccount
import com.tangem.data.jointaccount.createJointAccountDto
import com.tangem.datasource.api.jointaccount.models.JointAccountDto
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountConvertersTest {

    private val dtoConverter = JointAccountDtoConverter()
    private val dmConverter = JointAccountDMConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun convertDto(model: DtoModel) {
        // Act
        val actual = dtoConverter.convert(
            dto = createJointAccountDto(
                status = model.rawStatus,
                members = listOf(
                    JointAccountDto.Member(name = "Alice", address = FIXTURE_ALICE_ADDRESS, role = model.rawRole),
                ),
            ),
        )

        // Assert
        val expected = createJointAccount(
            status = model.expectedStatus,
            members = listOf(
                JointAccount.Member(
                    name = "Alice",
                    address = FIXTURE_ALICE_ADDRESS,
                    role = model.expectedRole,
                ),
            ),
            source = StatusSource.ACTUAL,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        DtoModel("pending", "creator", JointAccount.Status.PENDING, JointAccount.Role.CREATOR),
        DtoModel("confirming", "member", JointAccount.Status.CONFIRMING, JointAccount.Role.MEMBER),
        DtoModel("active", "creator", JointAccount.Status.ACTIVE, JointAccount.Role.CREATOR),
        // Values added by the backend later must degrade to UNKNOWN, never fail
        DtoModel("cancelled", "owner", JointAccount.Status.UNKNOWN, JointAccount.Role.UNKNOWN),
    )

    @Test
    fun `GIVEN account WHEN persistence round trip THEN restored equal except source becomes CACHE`() {
        // Arrange
        val original = createJointAccount(
            address = FIXTURE_ALICE_ADDRESS,
            status = JointAccount.Status.ACTIVE,
            source = StatusSource.ACTUAL,
        )

        // Act
        val restored = dmConverter.convertBack(dm = dmConverter.convert(account = original))

        // Assert
        assertThat(restored).isEqualTo(original.copy(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN unknown status WHEN persistence round trip THEN stays UNKNOWN`() {
        // Arrange
        val original = createJointAccount(status = JointAccount.Status.UNKNOWN)

        // Act
        val restored = dmConverter.convertBack(dm = dmConverter.convert(account = original))

        // Assert
        assertThat(restored.status).isEqualTo(JointAccount.Status.UNKNOWN)
    }

    data class DtoModel(
        val rawStatus: String,
        val rawRole: String,
        val expectedStatus: JointAccount.Status,
        val expectedRole: JointAccount.Role,
    )
}