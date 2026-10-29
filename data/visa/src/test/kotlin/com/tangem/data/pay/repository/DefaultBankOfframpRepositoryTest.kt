package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.pay.model.bankofframp.OfframpEta
import com.tangem.domain.pay.model.bankofframp.OfframpFee
import com.tangem.domain.pay.model.bankofframp.OfframpSource
import com.tangem.domain.pay.model.bankofframp.OfframpToken
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.BankAccountsResponse
import com.tangem.spend.datasource.pay.models.response.BankOfframpOptionsResponse
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@Suppress("UNCHECKED_CAST")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultBankOfframpRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestPerformer: TangemPayRequestPerformer = mockk()

    private val repository = DefaultBankOfframpRepository(
        tangemPayApi = tangemPayApi,
        requestPerformer = requestPerformer,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestPerformer)

        coEvery {
            requestPerformer.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
    }

    @Test
    fun `GIVEN two recipients WHEN getBankAccounts THEN both converted`() = runTest {
        // Arrange
        val response = BankAccountsResponse(result = listOf(account("a-1"), account("a-2")))
        coEvery { tangemPayApi.getBankAccounts(AUTH_HEADER) } returns ApiResponse.Success(response)

        // Act
        val actual = repository.getBankAccounts(WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.map { it.id }).containsExactly("a-1", "a-2").inOrder()
        coVerify(exactly = 1) { tangemPayApi.getBankAccounts(AUTH_HEADER) }
    }

    @Test
    fun `GIVEN account without details WHEN getBankAccounts THEN it is skipped`() = runTest {
        // Arrange
        val response = BankAccountsResponse(result = listOf(account("a-1"), account("a-2").copy(details = null)))
        coEvery { tangemPayApi.getBankAccounts(AUTH_HEADER) } returns ApiResponse.Success(response)

        // Act
        val actual = repository.getBankAccounts(WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.map { it.id }).containsExactly("a-1")
    }

    @Test
    fun `GIVEN no recipients WHEN getBankAccounts THEN empty list`() = runTest {
        // Arrange
        coEvery {
            tangemPayApi.getBankAccounts(AUTH_HEADER)
        } returns ApiResponse.Success(BankAccountsResponse(result = null))

        // Act
        val actual = repository.getBankAccounts(WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()).isEmpty()
    }

    @Test
    fun `GIVEN failing request WHEN getBankAccounts THEN unknown error`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getBankAccounts(AUTH_HEADER) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<BankAccountsResponse>

        // Act
        val actual = repository.getBankAccounts(WALLET_ID)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(BankOfframpError.Unknown::class.java)
    }

    @Test
    fun `GIVEN options WHEN getOptions THEN sources and destinations converted`() = runTest {
        // Arrange
        coEvery {
            tangemPayApi.getBankOfframpOptions(AUTH_HEADER, PAYMENT_ACCOUNT_TYPE)
        } returns ApiResponse.Success(optionsResponse())

        // Act
        val actual = repository.getOptions(WALLET_ID, product = TangemPayProduct.PAYMENT_ACCOUNT)

        // Assert
        val options = requireNotNull(actual.getOrNull())
        assertThat(options.sources.single().chainId).isEqualTo(137L)
        assertThat(options.destinations.single().rail).isEqualTo(BankRail.ACH)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN product WHEN getOptions THEN the options of its account type requested and converted`(
        model: AccountTypeModel,
    ) = runTest {
        // Arrange
        coEvery {
            tangemPayApi.getBankOfframpOptions(AUTH_HEADER, model.expectedAccountType)
        } returns ApiResponse.Success(optionsResponse())

        // Act
        val actual = repository.getOptions(WALLET_ID, product = model.product)

        // Assert
        assertThat(actual).isEqualTo(
            BankOfframpOptions(
                sources = listOf(
                    OfframpSource(
                        networkName = "polygon-pos",
                        isTestnet = false,
                        chainId = 137L,
                        tokens = listOf(
                            OfframpToken(
                                symbol = "USDC",
                                contractAddress = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359",
                            ),
                        ),
                    ),
                ),
                destinations = listOf(
                    OfframpDestination(
                        rail = BankRail.ACH,
                        fee = OfframpFee(
                            name = "ACH",
                            amount = BigDecimal("1.50"),
                            currency = "USD",
                            description = null,
                        ),
                        eta = OfframpEta(minDays = 1, maxDays = 3),
                    ),
                ),
            ).right(),
        )
        coVerify(exactly = 1) { tangemPayApi.getBankOfframpOptions(AUTH_HEADER, model.expectedAccountType) }
    }

    @Test
    fun `GIVEN response without a result WHEN getOptions THEN empty options`() = runTest {
        // Arrange
        coEvery {
            tangemPayApi.getBankOfframpOptions(AUTH_HEADER, PAYMENT_ACCOUNT_TYPE)
        } returns ApiResponse.Success(BankOfframpOptionsResponse(result = null))

        // Act
        val actual = repository.getOptions(WALLET_ID, product = TangemPayProduct.PAYMENT_ACCOUNT)

        // Assert
        val options = requireNotNull(actual.getOrNull())
        assertThat(options.sources).isEmpty()
        assertThat(options.destinations).isEmpty()
    }

    @Test
    fun `GIVEN failing request WHEN getOptions THEN unknown error`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getBankOfframpOptions(AUTH_HEADER, PAYMENT_ACCOUNT_TYPE) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<BankOfframpOptionsResponse>

        // Act
        val actual = repository.getOptions(WALLET_ID, product = TangemPayProduct.PAYMENT_ACCOUNT)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(BankOfframpError.Unknown::class.java)
    }

    private fun account(id: String) = BankAccountsResponse.BankAccount(
        id = id,
        displayName = "Primary checking account",
        details = BankAccountsResponse.Details(
            currency = "USD",
            rail = "ACH",
            isThirdParty = false,
            beneficiary = BankAccountsResponse.Beneficiary(
                type = "INDIVIDUAL",
                firstName = "John",
                lastName = "Doe",
                businessName = null,
            ),
            beneficiaryAddress = address(),
            bankName = "Chase Bank",
            bankAddress = address(),
            accountNumber = "1234567890",
            routingNumber = "021000021",
            bankAccountType = "CHECKING",
        ),
        createdAt = "2026-08-12T10:15:30Z",
    )

    private fun address() = BankAccountsResponse.Address(
        line1 = "123 Main St",
        line2 = null,
        city = "New York",
        region = "NY",
        postalCode = "10001",
        countryCode = "US",
    )

    private fun optionsResponse() = BankOfframpOptionsResponse(
        result = BankOfframpOptionsResponse.Result(
            sources = listOf(
                BankOfframpOptionsResponse.Source(
                    name = "polygon-pos",
                    isTestnet = false,
                    chainId = 137L,
                    tokens = listOf(
                        BankOfframpOptionsResponse.Token(
                            symbol = "USDC",
                            contractAddress = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359",
                        ),
                    ),
                ),
            ),
            destinations = listOf(
                BankOfframpOptionsResponse.Destination(
                    rail = "ACH",
                    fee = BankOfframpOptionsResponse.Fee(
                        name = "ACH",
                        calculationType = "FIXED",
                        amount = BigDecimal("1.50"),
                        currency = "USD",
                        description = null,
                    ),
                    eta = BankOfframpOptionsResponse.Eta(minDays = 1, maxDays = 3),
                ),
            ),
        ),
    )

    private fun provideTestModels() = listOf(
        AccountTypeModel(product = TangemPayProduct.PAYMENT_ACCOUNT, expectedAccountType = PAYMENT_ACCOUNT_TYPE),
        AccountTypeModel(product = TangemPayProduct.BANKING_ACCOUNT, expectedAccountType = "BANKING_ACCOUNT"),
    )

    internal data class AccountTypeModel(val product: TangemPayProduct, val expectedAccountType: String)

    private companion object {
        val WALLET_ID = UserWalletId("0123456789abcdef")
        const val AUTH_HEADER = "Bearer token"
        const val PAYMENT_ACCOUNT_TYPE = "PAYMENT_ACCOUNT"
    }
}