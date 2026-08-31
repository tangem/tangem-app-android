package com.tangem.domain.jointaccount.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountStatusValueConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a joint account WHEN convert THEN the lifecycle state the screen consumes`(model: TestModel) {
        // Act
        val actual = JointAccountStatusValueConverter.convert(model.account)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TestModel(
            account = account(status = JointAccount.Status.PENDING),
            expected = JointAccountStatusValue.Pending(source = StatusSource.CACHE, joinedMembers = 2, totalMembers = 3),
        ),
        TestModel(
            account = account(status = JointAccount.Status.CONFIRMING, safeAddress = SAFE),
            expected = JointAccountStatusValue.Confirming(source = StatusSource.CACHE, totalMembers = 3),
        ),
        TestModel(
            account = account(status = JointAccount.Status.ACTIVE, safeAddress = SAFE),
            expected = JointAccountStatusValue.Active(source = StatusSource.CACHE, totalMembers = 3, safeAddress = SAFE),
        ),
        TestModel(
            account = account(status = JointAccount.Status.ACTIVE, safeAddress = null),
            expected = JointAccountStatusValue.Error.Unavailable,
        ),
        TestModel(
            account = account(status = JointAccount.Status.CANCELLED, safeAddress = SAFE),
            expected = JointAccountStatusValue.Error.Unavailable,
        ),
        TestModel(
            account = account(status = JointAccount.Status.UNKNOWN),
            expected = JointAccountStatusValue.Error.Unavailable,
        ),
    )

    data class TestModel(val account: JointAccount, val expected: JointAccountStatusValue)

    private fun account(status: JointAccount.Status, safeAddress: String? = null) = JointAccount(
        cryptoAccountId = "A0",
        membersCount = 3,
        threshold = 2,
        safeAddress = safeAddress,
        status = status,
        members = listOf(
            JointAccount.Member(name = "Alice", address = "0x1", role = JointAccount.Role.CREATOR),
            JointAccount.Member(name = "Bob", address = "0x2", role = JointAccount.Role.MEMBER),
        ),
        source = StatusSource.CACHE,
    )

    private companion object {
        const val SAFE = "0x5afe000000000000000000000000000000000001"
    }
}