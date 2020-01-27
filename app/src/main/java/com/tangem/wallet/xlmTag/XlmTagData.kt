package com.tangem.wallet.xlmTag

import android.os.Bundle
import android.util.Log
import com.tangem.wallet.CoinData
import com.tangem.wallet.CoinEngine
import com.tangem.wallet.CoinEngine.InternalAmount
import org.stellar.sdk.KeyPair
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.LedgerResponse
import java.math.BigDecimal


class XlmTagData : CoinData() {
    class AccountResponseEx internal constructor(accountId: String?, sequenceNumber: Long?) : AccountResponse(KeyPair.fromAccountId(accountId), sequenceNumber)

    private var balance: CoinEngine.Amount? = null
    private var sequenceNumber: Long? = 0L
    private var baseReserve: CoinEngine.Amount? = CoinEngine.Amount("0.5", "XLM")
    var baseFee: CoinEngine.Amount? = CoinEngine.Amount("0.00001", "XLM")
        private set
    var isError404 = false
    var isTargetAccountCreated = false
    var fundsFromTrustedSource = false
    var fundsSentToTrustedSource = false


    override fun clearInfo() {
        super.clearInfo()
        balance = null
        isError404 = false
        isTargetAccountCreated = false
        fundsFromTrustedSource = false
        fundsSentToTrustedSource = false
    }

    fun getBalance(): CoinEngine.Amount? {
        return if (balance != null) {
            CoinEngine.Amount(balance!!.subtract(reserve), "XLM")
        } else {
            null
        }
    }

    val reserve: CoinEngine.Amount
        get() = CoinEngine.Amount(baseReserve!!.multiply(BigDecimal.valueOf(2)), "XLM")

    var accountResponse: AccountResponse
        get() = XlmTagData.AccountResponseEx(wallet, sequenceNumber)
        set(accountResponse) {
            if (accountResponse.balances.size > 0) {
                val balanceResponse = accountResponse.balances[0]
                balance = CoinEngine.Amount(balanceResponse.balance, "XLM")
            }
            sequenceNumber = accountResponse.sequenceNumber
            isBalanceReceived = true
        }

    fun setLedgerResponse(ledgerResponse: LedgerResponse) {
        val xlmEngine = XlmTagEngine()
        baseReserve = xlmEngine.convertToAmount(InternalAmount(ledgerResponse.baseReserveInStroops, "stroops"))
        baseFee = xlmEngine.convertToAmount(InternalAmount(ledgerResponse.baseFeeInStroops, "stroops"))
    }

    fun incSequenceNumber() {
        sequenceNumber = sequenceNumber?.inc()
    }

    override fun loadFromBundle(B: Bundle) {
        super.loadFromBundle(B)
        balance = if (B.containsKey("BalanceCurrency") && B.containsKey("BalanceDecimal")) {
            CoinEngine.Amount(B.getString("BalanceDecimal"), B.getString("BalanceCurrency"))
        } else {
            null
        }
        sequenceNumber = if (B.containsKey("sequenceNumber")) {
            B.getLong("sequenceNumber")
        } else {
            0L
        }
        baseReserve = if (B.containsKey("BaseReserveCurrency") && B.containsKey("BaseReserveDecimal")) {
            CoinEngine.Amount(B.getString("BaseReserveDecimal"), B.getString("BaseReserveCurrency"))
        } else {
            CoinEngine.Amount("0.5", "XLM")
        }
        baseFee = if (B.containsKey("BaseFeeCurrency") && B.containsKey("BaseFeeDecimal")) {
            CoinEngine.Amount(B.getString("BaseFeeDecimal"), B.getString("BaseFeeCurrency"))
        } else {
            CoinEngine.Amount("0.00001", "XLM")
        }
        if (B.containsKey("Error404")) isError404 = B.getBoolean("Error404") else isError404 = false
        if (B.containsKey("TargetAccountCreated")) isTargetAccountCreated = B.getBoolean("TargetAccountCreated") else isTargetAccountCreated = false
        fundsFromTrustedSource = if (B.containsKey("FundsFromTrustedSource")) {
            B.getBoolean("FundsFromTrustedSource")
        } else {
            false
        }
        fundsSentToTrustedSource = if (B.containsKey("FundsSentToTrustedSource")) {
            B.getBoolean("FundsSentToTrustedSource")
        } else {
            false
        }

    }

    override fun saveToBundle(B: Bundle) {
        super.saveToBundle(B)
        try {
            if (balance != null) {
                B.putString("BalanceCurrency", balance!!.currency)
                B.putString("BalanceDecimal", balance!!.toValueString())
            }
            if (sequenceNumber != null) {
                B.putLong("sequenceNumber", sequenceNumber!!)
            }
            if (baseReserve != null) {
                B.putString("BaseReserveCurrency", baseReserve!!.currency)
                B.putString("BaseReserveDecimal", baseReserve!!.toValueString())
            }
            if (baseFee != null) {
                B.putString("BaseFeeCurrency", baseFee!!.currency)
                B.putString("BaseFeeDecimal", baseFee!!.toValueString())
            }
            if (isError404) B.putBoolean("Error404", true)
            if (isTargetAccountCreated) B.putBoolean("TargetAccountCreated", true)
            if (fundsFromTrustedSource) B.putBoolean("FundsFromTrustedSource", true)
            if (fundsSentToTrustedSource) B.putBoolean("FundsSentToTrustedSource", true)

        } catch (e: Exception) {
            Log.e("Can't save to bundle ", e.message)
        }
    }
}