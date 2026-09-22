package com.tangem.data.collectibles.vault

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.core.error.ext.tangemError
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class GachaVaultErrorMappingTest {

    @Nested
    inner class ToEither {

        @Test
        fun `GIVEN a simple success WHEN toEither THEN unit is on the right`() {
            // Act
            val actual = SimpleResult.Success.toEither()

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
        }

        @Test
        fun `GIVEN a simple failure WHEN toEither THEN the same error is on the left`() {
            // Arrange
            val error = BlockchainSdkError.CustomError("rpc is down")

            // Act
            val actual = SimpleResult.Failure(error).toEither()

            // Assert
            assertThat(actual.leftOrNull()).isSameInstanceAs(error)
        }

        @Test
        fun `GIVEN a success WHEN toEither THEN the data is on the right`() {
            // Act
            val actual = Result.Success("vault").toEither()

            // Assert
            assertThat(actual).isEqualTo("vault".right())
        }

        @Test
        fun `GIVEN a failure WHEN toEither THEN the same error is on the left`() {
            // Arrange
            val error = BlockchainSdkError.CustomError("rpc is down")

            // Act
            val actual = Result.Failure(error).toEither()

            // Assert
            assertThat(actual.leftOrNull()).isSameInstanceAs(error)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BlockchainErrorToVaultFailure {

        private val coldWallet = mockk<UserWallet.Cold>()
        private val hotWallet = mockk<UserWallet.Hot>()

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a blockchain error WHEN toVaultFailure THEN it is mapped`(model: BlockchainErrorModel) {
            // Act
            val actual = model.error.toVaultFailure(model.userWallet)

            // Assert
            assertThat(actual.flatten()).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<BlockchainErrorModel> {
            val cardError = TangemSdkError.MissingPreflightRead()
            val hotSignerError = BlockchainSdkError.WrappedTangemError(
                tangemError = TangemSdkError.ExceptionError(IllegalStateException("wallet is locked")),
            )

            return listOf(coldWallet, hotWallet).flatMap { wallet ->
                listOf(
                    BlockchainErrorModel(
                        userWallet = wallet,
                        error = BlockchainSdkError.CustomError("rpc is down"),
                        expected = 102000005,
                    ),
                    BlockchainErrorModel(
                        userWallet = wallet,
                        error = BlockchainSdkError.UnsupportedOperation("Gacha vault is not supported"),
                        expected = GachaVaultError.NetworkUnsupported.errorCode,
                    ),
                    BlockchainErrorModel(
                        userWallet = wallet,
                        error = BlockchainSdkError.WrappedTangemError(tangemError = TangemSdkError.UserCancelled()),
                        expected = CollectiblesAccountError.Cancelled,
                    ),
                )
            } + listOf(
                BlockchainErrorModel(
                    userWallet = coldWallet,
                    error = BlockchainSdkError.WrappedTangemError(tangemError = cardError),
                    expected = CollectiblesAccountError.CardError(code = cardError.code, message = cardError.customMessage),
                ),
                BlockchainErrorModel(
                    userWallet = hotWallet,
                    error = hotSignerError,
                    expected = 102000000 + hotSignerError.code,
                ),
            )
        }
    }

    /** [expected] is the failure itself, or the universal code carried by a [CollectiblesAccountError.Vault]. */
    data class BlockchainErrorModel(val userWallet: UserWallet, val error: BlockchainError, val expected: Any)

    @Nested
    inner class BlockchainErrorToTangemError {

        @Test
        fun `GIVEN a card error WHEN toTangemError THEN the card's own error is kept`() {
            // Arrange
            val cardError = TangemSdkError.MissingPreflightRead()

            // Act
            val actual = BlockchainSdkError.WrappedTangemError(cardError).toTangemError()

            // Assert
            assertThat(actual).isSameInstanceAs(cardError)
        }

        @Test
        fun `GIVEN a chain error WHEN toTangemError THEN it leaves as a universal code`() {
            // Act
            val actual = BlockchainSdkError.CustomError("rpc is down").toTangemError()

            // Assert
            assertThat(actual).isNotInstanceOf(TangemSdkError::class.java)
            assertThat(actual.code).isEqualTo(102000005)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TangemErrorToVaultFailure {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a session error WHEN toVaultFailure THEN it is mapped`(model: TangemErrorModel) {
            // Act
            val actual = model.error.toVaultFailure()

            // Assert
            assertThat(actual.flatten()).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<TangemErrorModel> {
            val cardError = TangemSdkError.MissingPreflightRead()

            return listOf(
                TangemErrorModel(
                    error = GachaVaultError.WalletUnavailable.tangemError,
                    expected = GachaVaultError.WalletUnavailable.errorCode,
                ),
                TangemErrorModel(
                    error = TangemSdkError.UserCancelled(),
                    expected = CollectiblesAccountError.Cancelled,
                ),
                TangemErrorModel(
                    error = cardError,
                    expected = CollectiblesAccountError.CardError(code = cardError.code, message = cardError.customMessage),
                ),
            )
        }
    }

    data class TangemErrorModel(val error: TangemError, val expected: Any)

    private fun CollectiblesAccountError.flatten(): Any = when (this) {
        is CollectiblesAccountError.Vault -> error.errorCode
        else -> this
    }
}