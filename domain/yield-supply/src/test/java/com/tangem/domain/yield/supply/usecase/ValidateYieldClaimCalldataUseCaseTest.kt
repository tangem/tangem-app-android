package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldModuleAddressProvider
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.models.incentives.YieldClaimValidationError
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply.CHAIN_ID
import com.tangem.test.mock.yield.MockYieldSupply.MODULE_ADDRESS
import com.tangem.test.mock.yield.MockYieldSupply.REWARD_TOKEN
import com.tangem.test.mock.yield.MockYieldSupply.USER_ADDRESS
import com.tangem.test.mock.yield.MockYieldSupply.calldata
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidateYieldClaimCalldataUseCaseTest {

    private val yieldModuleAddressProvider: YieldModuleAddressProvider = mockk()
    private val network: Network = mockk()

    private val useCase = ValidateYieldClaimCalldataUseCase(
        yieldModuleAddressProvider = yieldModuleAddressProvider,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(yieldModuleAddressProvider)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: ValidationModel) = runTest {
        // Arrange
        coEvery {
            yieldModuleAddressProvider.getOrFetch(userWalletId = USER_WALLET_ID, network = network)
        } returns model.derivedModuleAddress

        // Act
        val actual = useCase(calldata = model.calldata, context = context(model.supportedRewardTokens))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN the address provider throws WHEN validated THEN the claim is refused instead of crashing`() = runTest {
        // Arrange
        coEvery {
            yieldModuleAddressProvider.getOrFetch(userWalletId = USER_WALLET_ID, network = network)
        } throws IllegalStateException("Wallet manager not found")

        // Act
        val actual = useCase(calldata = calldata(), context = context(SUPPORTED_REWARD_TOKENS))

        // Assert
        assertThat(actual).isEqualTo(YieldClaimValidationError.ModuleAddressUnavailable.left())
    }

    internal data class ValidationModel(
        val calldata: YieldClaimCalldata,
        val supportedRewardTokens: Set<String> = SUPPORTED_REWARD_TOKENS,
        val derivedModuleAddress: String? = MODULE_ADDRESS,
        val expected: Either<YieldClaimValidationError, YieldClaimCalldata>,
    )

    @Suppress("LongMethod")
    private fun provideTestModels(): List<ValidationModel> = listOf(
        ValidationModel(
            calldata = calldata(),
            expected = calldata().right(),
        ),
        ValidationModel(
            calldata = calldata(userAddress = USER_ADDRESS.uppercase(), to = MODULE_ADDRESS.uppercase()),
            expected = calldata(userAddress = USER_ADDRESS.uppercase(), to = MODULE_ADDRESS.uppercase()).right(),
        ),
        ValidationModel(
            calldata = calldata(),
            derivedModuleAddress = MODULE_ADDRESS.uppercase(),
            expected = calldata().right(),
        ),
        ValidationModel(
            calldata = calldata(hasClaimable = false),
            expected = YieldClaimValidationError.NothingToClaim.left(),
        ),
        ValidationModel(
            calldata = calldata(isClaimRecommended = false),
            expected = YieldClaimValidationError.NotRecommended.left(),
        ),
        ValidationModel(
            calldata = calldata(moduleAddress = null),
            expected = YieldClaimValidationError.IncompleteCalldata.left(),
        ),
        ValidationModel(
            calldata = calldata(to = null),
            expected = YieldClaimValidationError.IncompleteCalldata.left(),
        ),
        ValidationModel(
            calldata = calldata(data = null),
            expected = YieldClaimValidationError.IncompleteCalldata.left(),
        ),
        ValidationModel(
            calldata = calldata(data = "0x"),
            expected = YieldClaimValidationError.IncompleteCalldata.left(),
        ),
        ValidationModel(
            calldata = calldata(data = "0xdead"),
            expected = YieldClaimValidationError.IncompleteCalldata.left(),
        ),
        ValidationModel(
            calldata = calldata(chainId = 137),
            expected = YieldClaimValidationError.ContextMismatch.left(),
        ),
        ValidationModel(
            calldata = calldata(userAddress = "0xother"),
            expected = YieldClaimValidationError.ContextMismatch.left(),
        ),
        ValidationModel(
            calldata = calldata(to = "0xother"),
            expected = YieldClaimValidationError.DestinationMismatch.left(),
        ),
        // Both `to` and `moduleAddress` come from the same response, so agreeing with each other proves
        // nothing; only the module derived from the factory contract does.
        ValidationModel(
            calldata = calldata(moduleAddress = OTHER_MODULE, to = OTHER_MODULE),
            expected = YieldClaimValidationError.DestinationMismatch.left(),
        ),
        // The whole backend answer agrees with itself and still points somewhere the owner does not own.
        ValidationModel(
            calldata = calldata(),
            derivedModuleAddress = OTHER_MODULE,
            expected = YieldClaimValidationError.DestinationMismatch.left(),
        ),
        ValidationModel(
            calldata = calldata(),
            derivedModuleAddress = null,
            expected = YieldClaimValidationError.ModuleAddressUnavailable.left(),
        ),
        ValidationModel(
            calldata = calldata(),
            derivedModuleAddress = "",
            expected = YieldClaimValidationError.ModuleAddressUnavailable.left(),
        ),
        ValidationModel(
            calldata = calldata(moduleAddress = "", to = ""),
            expected = YieldClaimValidationError.IncompleteCalldata.left(),
        ),
        ValidationModel(
            calldata = calldata(data = "   "),
            expected = YieldClaimValidationError.IncompleteCalldata.left(),
        ),
        // A payload that is not hex passes a length check and only fails later, inside the claim call data parser.
        ValidationModel(
            calldata = calldata(data = "0xZZZZZZZZ"),
            expected = YieldClaimValidationError.IncompleteCalldata.left(),
        ),
        ValidationModel(
            calldata = calldata(data = "0xdeadbee"),
            expected = YieldClaimValidationError.IncompleteCalldata.left(),
        ),
        ValidationModel(
            calldata = calldata(rewardTokens = emptyList(), cumulativeAmounts = emptyList()),
            expected = YieldClaimValidationError.MalformedProof.left(),
        ),
        ValidationModel(
            calldata = calldata(cumulativeAmounts = listOf("1", "2")),
            expected = YieldClaimValidationError.MalformedProof.left(),
        ),
        // The reward the service reports as unroutable is absent from the admitted set.
        ValidationModel(
            calldata = calldata(),
            supportedRewardTokens = setOf(SECOND_REWARD_TOKEN),
            expected = YieldClaimValidationError.UnsupportedRewardToken.left(),
        ),
        // The wrapped-reward case: the payload pays out a token the incentives response says nothing about.
        ValidationModel(
            calldata = calldata(rewardTokens = listOf(REWARD_TOKEN, "0xwrapped"), cumulativeAmounts = listOf("1", "2")),
            expected = YieldClaimValidationError.UnsupportedRewardToken.left(),
        ),
        ValidationModel(
            calldata = calldata(),
            supportedRewardTokens = emptySet(),
            expected = YieldClaimValidationError.UnsupportedRewardToken.left(),
        ),
        ValidationModel(
            calldata = calldata(rewardTokens = listOf(REWARD_TOKEN.uppercase()), cumulativeAmounts = listOf("100")),
            expected = calldata(
                rewardTokens = listOf(REWARD_TOKEN.uppercase()),
                cumulativeAmounts = listOf("100"),
            ).right(),
        ),
        // A token that has nothing claimable right now is still admitted: the cumulative proof keeps listing it.
        ValidationModel(
            calldata = calldata(),
            supportedRewardTokens = SUPPORTED_REWARD_TOKENS + "0xdrained",
            expected = calldata().right(),
        ),
    )

    private fun context(supportedRewardTokens: Set<String>) = YieldClaimValidationContext(
        userWalletId = USER_WALLET_ID,
        network = network,
        chainId = CHAIN_ID,
        userAddress = USER_ADDRESS,
        moduleAddress = MODULE_ADDRESS,
        supportedRewardTokens = supportedRewardTokens,
    )

    private companion object {
        const val OTHER_MODULE = "0xothermodule"
        const val SECOND_REWARD_TOKEN = "0xreward2"
        val SUPPORTED_REWARD_TOKENS = setOf(REWARD_TOKEN.lowercase(), SECOND_REWARD_TOKEN)
        val USER_WALLET_ID = UserWalletId("0011")
    }
}