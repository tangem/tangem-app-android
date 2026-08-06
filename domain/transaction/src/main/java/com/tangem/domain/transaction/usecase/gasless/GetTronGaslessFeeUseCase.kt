package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.domain.transaction.raiseIllegalStateError

/**
 * Fetches the Tron gasless compensation quote (eager estimate) for an original token transfer and
 * exposes it as a [TransactionFeeExtended] whose displayed fee is the compensation amount in the fee
 * token. The carried [TransactionFeeExtended.tronGaslessQuote] routes the send step to the Tron flow.
 *
 * Tron does not use the EVM gasless machinery — this is a parallel, thin path. The fee is returned
 * regardless of balance: insufficiency is surfaced as a send notification (see
 * GetBalanceNotEnoughForFeeWarningUseCase), so the fee row keeps showing the amount while Send stays
 * blocked.
 */
class GetTronGaslessFeeUseCase(
    private val tronGaslessTransactionRepository: TronGaslessTransactionRepository,
) {

    suspend operator fun invoke(
        transactionData: TransactionData,
        feeToken: CryptoCurrency.Token,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
        catch(
            block = {
                val uncompiled = transactionData.requireUncompiled()
                val amountBaseUnits = uncompiled.amount.value
                    ?.movePointRight(uncompiled.amount.decimals)
                    ?.toBigInteger()
                    ?.toString()
                    ?: raiseIllegalStateError("Tron gasless: transaction amount is null")

                val quote = tronGaslessTransactionRepository.estimate(
                    TronGaslessEstimateParams(
                        fromAddress = uncompiled.sourceAddress,
                        toAddress = uncompiled.destinationAddress,
                        tokenContract = uncompiled.contractAddress ?: feeToken.contractAddress,
                        amount = amountBaseUnits,
                        feeTokenContract = feeToken.contractAddress,
                    ),
                )

                // Derived from the raw base-unit amount (the compensation transfer uses the same
                // value), so the displayed and charged fee are always identical.
                val compensationValue = quote.compensationAmountRaw.toBigDecimal()
                    .movePointLeft(feeToken.decimals)

                val feeAmount = Amount(
                    token = Token(
                        symbol = feeToken.symbol,
                        contractAddress = feeToken.contractAddress,
                        decimals = feeToken.decimals,
                    ),
                    value = compensationValue,
                )

                TransactionFeeExtended(
                    transactionFee = TransactionFee.Single(normal = Fee.Common(amount = feeAmount)),
                    feeTokenId = feeToken.id,
                    tronGaslessQuote = quote,
                )
            },
            catch = { raise(GaslessError.DataError(it)) },
        )
    }
}