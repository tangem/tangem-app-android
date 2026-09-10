package com.tangem.data.jointaccount.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.jointaccount.FIXTURE_ALICE_ADDRESS
import com.tangem.data.jointaccount.createJointAccount
import com.tangem.data.jointaccount.createJointAccountDto
import com.tangem.data.jointaccount.createJointAccountInvitePreview
import com.tangem.data.jointaccount.createJointAccountInvitePreviewDto
import com.tangem.data.jointaccount.createJointOperation
import com.tangem.data.jointaccount.createSafeTx
import com.tangem.store.datasource.jointaccount.models.JointAccountDto
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.models.StatusSource
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountConvertersTest {

    private val operationConverter = JointOperationConverter(safeTxConverter = SafeTxConverter())
    private val dtoConverter = JointAccountDtoConverter(operationConverter = operationConverter)
    private val dmConverter = JointAccountDMConverter()
    private val invitePreviewConverter = JointAccountInvitePreviewDtoConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun convertDto(model: DtoModel) {
        // Act
        val actual = dtoConverter.convert(
            value = createJointAccountDto(
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
                    address = FIXTURE_ALICE_ADDRESS.lowercase(),
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
        DtoModel("cancelled", "member", JointAccount.Status.CANCELLED, JointAccount.Role.MEMBER),
        // Values added by the backend later must degrade to UNKNOWN, never fail
        DtoModel("frozen", "owner", JointAccount.Status.UNKNOWN, JointAccount.Role.UNKNOWN),
    )

    @Test
    fun `GIVEN account WHEN persistence round trip THEN restored equal except source becomes CACHE`() {
        // Arrange
        val original = createJointAccount(
            safeAddress = FIXTURE_ALICE_ADDRESS,
            status = JointAccount.Status.ACTIVE,
            source = StatusSource.ACTUAL,
        )

        // Act
        val restored = dmConverter.convertBack(value = dmConverter.convert(value = original))

        // Assert
        assertThat(restored).isEqualTo(original.copy(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN account with in-flight operations WHEN persistence round trip THEN operations survive`() {
        // Arrange
        val original = createJointAccount(
            status = JointAccount.Status.ACTIVE,
            source = StatusSource.ACTUAL,
            inFlightOperations = mapOf(
                1L to createJointOperation(
                    chainId = 1,
                    safeTx = createSafeTx(value = BigInteger(UINT256_MAX)),
                ),
                137L to createJointOperation(chainId = 137, status = JointOperation.Status.PENDING),
            ),
        )

        // Act
        val restored = dmConverter.convertBack(value = dmConverter.convert(value = original))

        // Assert
        assertThat(restored).isEqualTo(original.copy(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN unknown status WHEN persistence round trip THEN stays UNKNOWN`() {
        // Arrange
        val original = createJointAccount(status = JointAccount.Status.UNKNOWN)

        // Act
        val restored = dmConverter.convertBack(value = dmConverter.convert(value = original))

        // Assert
        assertThat(restored.status).isEqualTo(JointAccount.Status.UNKNOWN)
    }

    @Test
    fun `GIVEN invite preview dto WHEN convert THEN domain model is returned`() {
        // Act
        val actual = invitePreviewConverter.convert(value = createJointAccountInvitePreviewDto())

        // Assert
        assertThat(actual).isEqualTo(createJointAccountInvitePreview())
    }

    data class DtoModel(
        val rawStatus: String,
        val rawRole: String,
        val expectedStatus: JointAccount.Status,
        val expectedRole: JointAccount.Role,
    )

    private companion object {
        const val UINT256_MAX =
            "115792089237316195423570985008687907853269984665640564039457584007913129639935"
    }
}