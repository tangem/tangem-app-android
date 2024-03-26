package com.tangem.lib.visa

import arrow.fx.coroutines.parZip
import com.tangem.lib.visa.model.VisaBalancesAndLimits
import com.tangem.lib.visa.model.VisaBalancesAndLimits.Balances
import com.tangem.lib.visa.model.VisaBalancesAndLimits.Limits
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

    override suspend fun getBalancesAndLimits(walletAddress: String): VisaBalancesAndLimits {
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
    ): VisaBalancesAndLimits = parZip(
        dispatchers.io,
        { fetchBalances(paymentAccount, paymentToken) },
        { fetchLimits(paymentAccount, paymentToken) },
        { balances, (oldLimit, newLimit, changeDate) ->
            VisaBalancesAndLimits(balances, oldLimit, newLimit, changeDate)
        },
    )

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
            { paymentAccount.pendingRefundTotal().send() },
        ) { total, verified, payment, withdrawal, debtPayment, blocked, debt, refund ->
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
                pendingRefund = refund.toBigDecimal(decimals),
            )
        }
    }

    private fun fetchLimits(
        paymentAccount: TangemPaymentAccount,
        paymentToken: PaymentTokenInfo,
    ): Triple<Limits, Limits, Instant> {
        val (
            oldLimit,
            newLimit,
            changeDateSeconds,
        ) = paymentAccount.limits().send()

        return Triple(
            first = getLimits(oldLimit, paymentToken),
            second = getLimits(newLimit, paymentToken),
            third = changeDateSeconds.toInstant(),
        )
    }

    private fun getLimits(limit: TangemPaymentAccount.Limits, paymentToken: PaymentTokenInfo): Limits = Limits(
        spendLimit = limit._01_spendLimit.toLimit(paymentToken.decimals),
        noOtpLimit = limit._02_noOtpSpendLimit.toLimit(paymentToken.decimals),
        singleTransactionLimit = limit._00_singleTransactionLimit.toBigDecimal(paymentToken.decimals),
        expirationDate = limit._03_spendLimitsTimer.expireTimestamp.toInstant(),
        spendPeriodSeconds = limit._04_spendLimitsPeriod,
    )

    private fun TangemPaymentAccount.Limit.toLimit(decimals: Int): Limits.Limit {
        return Limits.Limit(
            limit = _00_limit.toBigDecimal(decimals),
            spent = _01_spent.toBigDecimal(decimals),
        )
    }

    private data class PaymentTokenInfo(
        val decimals: Int,
        val contract: ERC20,
    )
}