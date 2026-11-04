package com.tangem.data.walletmanager.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RecentTransactionOwnershipTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val coin = currencyFactory.createCoin(Blockchain.Polygon)
    private val usdc = currencyFactory.createToken(
        blockchain = Blockchain.Polygon,
        contractAddress = USDC_CONTRACT,
        symbol = "USDC",
        decimals = 6,
    )
    private val usdt = currencyFactory.createToken(
        blockchain = Blockchain.Polygon,
        contractAddress = USDT_CONTRACT,
        symbol = "USDT",
        decimals = 6,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun belongsToHistoryOf(model: OwnershipModel) {
        // Act
        val actual = model.transaction.belongsToHistoryOf(model.currency)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        // An approval is denominated in the approved token but belongs to the coin's history, where the explorer
        // reports it — the token's history carries Transfer events only.
        OwnershipModel(transaction = approval(), currency = coin, expected = true),
        OwnershipModel(transaction = approval(), currency = usdc, expected = false),
        OwnershipModel(transaction = approval(), currency = usdt, expected = false),
        // A claim is built in the yield token but is the coin's own call; the reward reaches the token's history
        // from the explorer.
        OwnershipModel(transaction = merklClaim(), currency = coin, expected = true),
        OwnershipModel(transaction = merklClaim(), currency = usdc, expected = false),
        OwnershipModel(transaction = merklClaim(isUpgradeWrapped = true), currency = coin, expected = true),
        OwnershipModel(transaction = merklClaim(isUpgradeWrapped = true), currency = usdc, expected = false),
        // Plain transfers keep being matched by their own amount.
        OwnershipModel(transaction = coinTransfer(), currency = coin, expected = true),
        OwnershipModel(transaction = coinTransfer(), currency = usdc, expected = false),
        OwnershipModel(transaction = tokenTransfer(), currency = usdc, expected = true),
        OwnershipModel(transaction = tokenTransfer(), currency = usdt, expected = false),
        OwnershipModel(transaction = tokenTransfer(), currency = coin, expected = false),
        // A Yield Mode enter or exit names no contract address, yet moves the token, so it belongs to the token.
        OwnershipModel(transaction = yieldEnter(), currency = usdc, expected = true),
        OwnershipModel(transaction = yieldEnter(), currency = usdt, expected = false),
        OwnershipModel(transaction = yieldEnter(), currency = coin, expected = false),
        OwnershipModel(transaction = yieldEnter(isUpgradeWrapped = true), currency = usdc, expected = true),
        OwnershipModel(transaction = yieldExit(), currency = usdc, expected = true),
        OwnershipModel(transaction = yieldExit(), currency = usdt, expected = false),
        OwnershipModel(transaction = yieldExit(), currency = coin, expected = false),
        // The other module calls move no token, so the token's history never reports them.
        OwnershipModel(transaction = yieldInitToken(), currency = usdc, expected = false),
    )

    internal data class OwnershipModel(
        val transaction: TransactionData.Uncompiled,
        val currency: CryptoCurrency,
        val expected: Boolean,
    )

    /** The approval the swap flow builds: a zero amount of the approved token, sent to that token's contract. */
    private fun approval(): TransactionData.Uncompiled = transaction(
        amount = Amount(token = sdkUsdc, value = BigDecimal.ZERO),
        destinationAddress = USDC_CONTRACT,
        extras = EthereumTransactionExtras(
            callData = ApprovalERC20TokenCallData(spenderAddress = SPENDER_ADDRESS, amount = null),
        ),
    )

    private fun merklClaim(isUpgradeWrapped: Boolean = false): TransactionData.Uncompiled {
        val callData = MerklClaimCallData.fromHex(MERKL_CLAIM_CALL_DATA)
        return transaction(
            amount = Amount(token = sdkUsdc, value = BigDecimal.ZERO),
            destinationAddress = YIELD_MODULE_ADDRESS,
            extras = EthereumTransactionExtras(callData = if (isUpgradeWrapped) upgradeWrapped(callData) else callData),
        )
    }

    private fun yieldEnter(isUpgradeWrapped: Boolean = false): TransactionData.Uncompiled {
        val callData = EthereumYieldSupplyEnterCallData(tokenContractAddress = USDC_CONTRACT)
        return yieldCall(callData = if (isUpgradeWrapped) upgradeWrapped(callData) else callData)
    }

    private fun yieldExit(): TransactionData.Uncompiled =
        yieldCall(callData = EthereumYieldSupplyExitCallData(tokenContractAddress = USDC_CONTRACT))

    private fun yieldInitToken(): TransactionData.Uncompiled = yieldCall(
        callData = EthereumYieldSupplyInitTokenCallData(
            tokenContractAddress = USDC_CONTRACT,
            maxNetworkFee = Amount(token = sdkUsdc, value = BigDecimal.ONE),
        ),
    )

    /** A module call as the Yield Mode flow builds it: a zero yield amount of the token, sent to the module. */
    private fun yieldCall(callData: SmartContractCallData): TransactionData.Uncompiled = transaction(
        amount = Amount(
            currencySymbol = sdkUsdc.symbol,
            value = BigDecimal.ZERO,
            decimals = sdkUsdc.decimals,
            type = AmountType.TokenYieldSupply(
                token = sdkUsdc,
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
            ),
        ),
        destinationAddress = YIELD_MODULE_ADDRESS,
        extras = EthereumTransactionExtras(callData = callData),
    )

    private fun upgradeWrapped(callData: SmartContractCallData): SmartContractCallData =
        EthereumYieldSupplyUpgradeToAndCallCallData(newImplementation = LATEST_IMPLEMENTATION, innerCallData = callData)

    private fun coinTransfer(): TransactionData.Uncompiled = transaction(
        amount = Amount(value = BigDecimal.ONE, blockchain = Blockchain.Polygon),
        destinationAddress = RECIPIENT_ADDRESS,
    )

    private fun tokenTransfer(): TransactionData.Uncompiled = transaction(
        amount = Amount(token = sdkUsdc, value = BigDecimal.ONE),
        destinationAddress = RECIPIENT_ADDRESS,
    )

    private fun transaction(
        amount: Amount,
        destinationAddress: String,
        extras: TransactionExtras? = null,
    ): TransactionData.Uncompiled = TransactionData.Uncompiled(
        amount = amount,
        fee = null,
        sourceAddress = OWN_ADDRESS,
        destinationAddress = destinationAddress,
        extras = extras,
    )

    private val sdkUsdc = Token(name = "USD Coin", symbol = "USDC", contractAddress = USDC_CONTRACT, decimals = 6)

    private companion object {
        const val LATEST_IMPLEMENTATION = "0x5555555555555555555555555555555555555555"
        const val USDC_CONTRACT = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
        const val USDT_CONTRACT = "0xc2132D05D31c914a87C6611C10748AEb04B58e8F"
        const val SPENDER_ADDRESS = "0x1111111254EEB25477B68fb85Ed929f73A960582"
        const val RECIPIENT_ADDRESS = "0x2222222222222222222222222222222222222222"
        const val OWN_ADDRESS = "0x3333333333333333333333333333333333333333"
        const val YIELD_MODULE_ADDRESS = "0x4444444444444444444444444444444444444444"
        const val MERKL_CLAIM_CALL_DATA = "0xb4701e3e0001"
    }
}