package com.tangem.lib.visa

import arrow.fx.coroutines.parZip
import com.tangem.lib.visa.model.VisaContractInfo
import com.tangem.lib.visa.model.VisaContractInfo.*
import com.tangem.lib.visa.utils.toBigDecimal
import com.tangem.lib.visa.utils.toInstant
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import org.joda.time.Instant
import org.web3j.protocol.Web3j
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider

internal class DefaultVisaContractInfoProvider(
    private val web3j: Web3j,
    private val transactionManager: TransactionManager,
    private val gasProvider: ContractGasProvider,
    private val bridgeProcessorAddress: String,
    private val paymentAccountRegistryAddress: String,
    private val dispatchers: CoroutineDispatcherProvider,
) : VisaContractInfoProvider {

    override suspend fun getContractInfo(walletAddress: String): VisaContractInfo {
        return parZip(
            dispatchers.io,
            { loadPaymentAccount(walletAddress) },
            { loadPaymentTokenInfo() },
            { paymentAccount, paymentToken ->
                fetchBalancesAndLimits(paymentAccount, paymentToken)
            },
        )
    }

    private fun loadPaymentAccount(walletAddress: String): TangemPaymentAccount {
        val paymentAccountRegistry = TangemPaymentAccountRegistry.load(
            /* contractAddress = */ paymentAccountRegistryAddress,
            /* web3j = */ web3j,
            /* transactionManager = */ transactionManager,
            /* contractGasProvider = */ gasProvider,
        )
        val paymentAccountAddress = paymentAccountRegistry.paymentAccountByCard(walletAddress).send()

        return TangemPaymentAccount.load(paymentAccountAddress, web3j, transactionManager, gasProvider)
    }

    private fun loadPaymentTokenInfo(): PaymentTokenInfo {
        val tangemBridgeProcessor = TangemBridgeProcessor.load(
            /* contractAddress = */ bridgeProcessorAddress,
            /* web3j = */ web3j,
            /* transactionManager = */ transactionManager,
            /* contractGasProvider = */ gasProvider,
        )
        val paymentTokenContractAddress = tangemBridgeProcessor.paymentToken().send()
        val paymentTokenContract = ERC20.load(paymentTokenContractAddress, web3j, transactionManager, gasProvider)
        val paymentTokenDecimals = paymentTokenContract.decimals().send()

        return PaymentTokenInfo(
            decimals = paymentTokenDecimals.toInt(),
            contract = paymentTokenContract,
        )
    }

    private suspend fun fetchBalancesAndLimits(
        paymentAccount: TangemPaymentAccount,
        paymentToken: PaymentTokenInfo,
    ): VisaContractInfo = parZip(
        dispatchers.io,
        { fetchToken(paymentAccount) },
        { fetchBalances(paymentAccount, paymentToken) },
        { fetchLimits(paymentAccount, paymentToken) },
        { token, balances, (oldLimit, newLimit, changeDate) ->
            VisaContractInfo(token, balances, oldLimit, newLimit, changeDate)
        },
    )

    private suspend fun fetchToken(paymentAccount: TangemPaymentAccount): Token {
        val paymentTokenContractAddress = paymentAccount.paymentToken().send()
        val paymentTokenContract = ERC20.load(paymentTokenContractAddress, web3j, transactionManager, gasProvider)

        return parZip(
            dispatchers.io,
            { paymentTokenContract.name().send() },
            { paymentTokenContract.symbol().send() },
            { paymentTokenContract.decimals().send() },
        ) { name, symbol, decimals ->
            Token(
                name = name,
                symbol = symbol,
                decimals = decimals.toInt(),
                address = paymentTokenContractAddress,
            )
        }
    }

    private suspend fun fetchBalances(paymentAccount: TangemPaymentAccount, paymentToken: PaymentTokenInfo): Balances {
        return parZip(
            dispatchers.io,
            { paymentToken.contract.balanceOf(paymentAccount.contractAddress).send() },
            { paymentAccount.verifiedBalance().send() },
            { paymentAccount.availableForPayment().send() },
            { paymentAccount.availableForWithdrawal().send() },
            { paymentAccount.availableForDebtPayment().send() },
            { paymentAccount.blockedAmount().send() },
            { paymentAccount.debtAmount().send() },
        ) { total, verified, payment, withdrawal, debtPayment, blocked, debt ->
            val decimals = paymentToken.decimals

            Balances(
                total = total.toBigDecimal(decimals),
                verified = verified.toBigDecimal(decimals),
                available = Balances.Available(
                    forPayment = payment.toBigDecimal(decimals),
                    forWithdrawal = withdrawal.toBigDecimal(decimals),
                    forDebtPayment = debtPayment.toBigDecimal(decimals),
                ),
                blocked = blocked.toBigDecimal(decimals),
                debt = debt.toBigDecimal(decimals),
            )
        }
    }

    @Suppress("UnusedPrivateMember")
    private fun fetchLimits(
        paymentAccount: TangemPaymentAccount,
        paymentToken: PaymentTokenInfo,
    ): Triple<Limits, Limits, Instant> {
        TODO() // TODO: [REDACTED_TASK_KEY]
        // val (
        //     oldLimit,
        //     newLimit,
        //     changeDateSeconds,
        // ) = paymentAccount.cards("").send().component5()
        //
        // return Triple(
        //     first = getLimits(oldLimit, paymentToken),
        //     second = getLimits(newLimit, paymentToken),
        //     third = changeDateSeconds.toInstant(),
        // )
    }

    @Suppress("UnusedPrivateMember")
    private fun getLimits(limit: TangemPaymentAccount.Limits, paymentToken: PaymentTokenInfo): Limits = Limits(
        spendLimit = limit.spendLimit.toLimit(paymentToken.decimals),
        noOtpLimit = limit.noConfirmationSpendLimit.toLimit(paymentToken.decimals),
        singleTransactionLimit = limit.singleTransactionLimit.toBigDecimal(paymentToken.decimals),
        expirationDate = limit.spendLimitsTimer.expireTimestamp.toInstant(),
        spendPeriodSeconds = limit.spendLimitsPeriod,
    )

    private fun TangemPaymentAccount.Limit.toLimit(decimals: Int): Limits.Limit {
        return Limits.Limit(
            limit = limit.toBigDecimal(decimals),
            spent = spent.toBigDecimal(decimals),
        )
    }

    private data class PaymentTokenInfo(
        val decimals: Int,
        val contract: ERC20,
    )
}