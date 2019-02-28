package com.tangem.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tangem.card_common.data.TangemCard
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class PendingTransactionsStorage
(
        val context: Context
) {
    private lateinit var cardTransactions: MutableMap<String, CardTransactionsInfo>
    private val transactionsFile: File = File(context.filesDir, "transactions.json")

    private var cacheDir: File? = null

    init {
        cacheDir = File(context.filesDir, "artworks")
        if (!cacheDir!!.exists())
            cacheDir!!.mkdirs()

        if (transactionsFile.exists()) {
            try {
                transactionsFile.bufferedReader().use { cardTransactions = Gson().fromJson(it, object : TypeToken<HashMap<String, CardTransactionsInfo>>() {}.type) }
            } catch (e: Exception) {
                e.printStackTrace()
                cardTransactions = HashMap()
            }
        } else {
            cardTransactions = HashMap()
        }
    }

    private fun clearExpired()
    {
        for(cardId in cardTransactions.keys) {
            cardTransactions[cardId]!!.transactions.removeAll { it.isExpired() }
        }
        cardTransactions.entries.removeAll { it.value.isEmpty() }
    }

    fun save() {
        clearExpired()
        val sTransactions = Gson().toJson(cardTransactions)

        transactionsFile.bufferedWriter().use { it.write(sTransactions) }
    }

    fun putTransaction(card: TangemCard, txId: String, expireTimeoutInSeconds: Int) {
        val sendDate=Date()
        val calendar=Calendar.getInstance()
        calendar.time=sendDate
        calendar.add(Calendar.SECOND, expireTimeoutInSeconds)
        val expireDate=calendar.time
        putTransaction(card.cidDescription, txId, sendDate, expireDate)
    }

    private fun putTransaction(cardId: String, txId: String, sendDate: Date, expireDate: Date) {
        val transactionInfo = TransactionInfo(
                txId, sendDate, expireDate
        )
        if (cardTransactions[cardId] == null) {
            cardTransactions[cardId] = CardTransactionsInfo(arrayListOf())
        }
        cardTransactions[cardId]?.transactions?.add(transactionInfo)
        save()
    }

    fun getTransactions(card: TangemCard): CardTransactionsInfo? {
        clearExpired()
        return cardTransactions[card.cidDescription]
    }

    fun hasTransactions(card: TangemCard): Boolean {
        val cardTransactionsInfo=getTransactions(card)
        if( cardTransactionsInfo!=null ) return cardTransactionsInfo.transactions.count()>0
        return false
    }

    fun removeTransaction(card: TangemCard, txId: String)
    {
        cardTransactions[card.cidDescription]?.transactions?.removeAll { it.tx==txId }
        save()
    }

    data class TransactionInfo(
            val tx: String,
            val sendDate: Date,
            val expireDate: Date
    ) {
        fun isExpired(): Boolean {
            return Date().after(expireDate)
        }
    }

    data class CardTransactionsInfo(
            var transactions: MutableList<TransactionInfo>
    ) {
        fun isEmpty(): Boolean {
            return transactions.count()==0
        }
    }
}