package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.domain.models.account.CryptoPortfolioIcon.Color
import com.tangem.domain.models.account.CryptoPortfolioIcon.Icon
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verifyOrder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoPortfolioIconTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OfMainAccount {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun ofMainAccount(model: OfMainAccountModel) {
            // Act
            val actual = CryptoPortfolioIcon.ofMainAccount(userWalletId = model.userWalletId)

            // Assert
            val expectedColor = model.expectedColor
            Truth.assertThat(actual.color).isEqualTo(expectedColor)

            val expectedIcon = Icon.Star
            Truth.assertThat(actual.value).isEqualTo(expectedIcon)
        }

        private fun provideTestModels() = listOf(
            OfMainAccountModel(
                userWalletId = UserWalletId("1234567890abcdef"),
                expectedColor = Color.Pattypan,
            ),
            OfMainAccountModel(
                userWalletId = UserWalletId("27163F47405CE73110837F24DF82607FF11C7AF9D78C93F409E4FEAFF3400C8F"),
                expectedColor = Color.CandyGrapeFizz,
            ),
            OfMainAccountModel(
                userWalletId = UserWalletId("64A3791C180584C700EBECD6EAB36CBC34643BB449BC87761104C09F41DBCF3D"),
                expectedColor = Color.PalatinateBlue,
            ),
            OfMainAccountModel(
                userWalletId = UserWalletId("01C061A99FCCEDA87933267EBAB3513592F83AD2E27BDA6EE5546BA96009D21F"),
                expectedColor = Color.Pelati,
            ),
            OfMainAccountModel(
                userWalletId = UserWalletId("6D387A8FA5D2AF95F601EBCA8736D73D2ED53159835D8C407FBD4BBB10290C8B"),
                expectedColor = Color.CaribbeanBlue,
            ),
            OfMainAccountModel(
                userWalletId = UserWalletId("33FCD9B9982C31648C235AE55A29212D567ECD3BA24BE4227D1A01897ADBC959"),
                expectedColor = Color.SweetDesire,
            ),
            OfMainAccountModel(
                userWalletId = UserWalletId("197C8C5AA59270F3E9E1F30799A007D193DA596E6DC24C37D002C2EC203C2A0B"),
                expectedColor = Color.VitalGreen,
            ),
            OfMainAccountModel(
                userWalletId = UserWalletId("ACF90C18393828958B5E795771F0692A00D3D7ADC092F726AB4A7E3116DD6E6E"),
                expectedColor = Color.Pattypan,
            ),
        )
    }

    data class OfMainAccountModel(
        val userWalletId: UserWalletId,
        val expectedColor: Color,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OfDefaultCustomAccount {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun ofCustomAccount(model: OfDefaultCustomAccountModel) {
            // Arrange
            val availableIcons = Icon.entries - setOf(Icon.Letter, Icon.Star)

            mockkObject(Random.Default)

            every { Random.nextInt(until = availableIcons.size) } returns model.randomIconIndex
            every { Random.nextInt(until = Color.entries.size) } returns model.randomColorIndex

            // Act
            val actual = CryptoPortfolioIcon.ofDefaultCustomAccount()

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)

            verifyOrder {
                Random.nextInt(until = availableIcons.size)
                Random.nextInt(until = Color.entries.size)
            }

            unmockkObject(Random.Default)
        }

        private fun provideTestModels() = listOf(
            OfDefaultCustomAccountModel(
                randomIconIndex = 0,
                randomColorIndex = 0,
                expected = CryptoPortfolioIcon.ofCustomAccount(value = Icon.User, color = Color.Azure),
            ),
            OfDefaultCustomAccountModel(
                randomIconIndex = 1,
                randomColorIndex = 1,
                expected = CryptoPortfolioIcon.ofCustomAccount(value = Icon.Family, color = Color.CaribbeanBlue),
            ),
            OfDefaultCustomAccountModel(
                randomIconIndex = Icon.entries.lastIndex - 2,
                randomColorIndex = Color.entries.lastIndex,
                expected = CryptoPortfolioIcon.ofCustomAccount(value = Icon.Gift, color = Color.VitalGreen),
            ),
        )
    }

    data class OfDefaultCustomAccountModel(
        val randomIconIndex: Int,
        val randomColorIndex: Int,
        val expected: CryptoPortfolioIcon,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OfCustomAccountWithTypeAndColor {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun ofCustomAccount(model: OfCustomAccountModel) {
            // Act
            val actual = CryptoPortfolioIcon.ofCustomAccount(value = model.icon, color = model.color)

            // Assert
            val expectedColor = model.expectedColor
            Truth.assertThat(actual.color).isEqualTo(expectedColor)

            val expectedType = model.expectedType
            Truth.assertThat(actual.value).isEqualTo(expectedType)
        }

        private fun provideTestModels() = listOf(
            OfCustomAccountModel(
                icon = Icon.User,
                color = Color.CaribbeanBlue,
                expectedType = Icon.User,
                expectedColor = Color.CaribbeanBlue,
            ),
            OfCustomAccountModel(
                icon = Icon.Letter,
                color = Color.DullLavender,
                expectedType = Icon.Letter,
                expectedColor = Color.DullLavender,
            ),
            OfCustomAccountModel(
                icon = Icon.Star,
                color = Color.DullLavender,
                expectedType = Icon.Star,
                expectedColor = Color.DullLavender,
            ),
        )
    }

    data class OfCustomAccountModel(
        val icon: Icon,
        val color: Color,
        val expectedType: Icon,
        val expectedColor: Color,
    )
}