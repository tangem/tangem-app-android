package com.tangem.common

import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.tangem.CardValues
import com.tangem.CardValuesEntityQueries
import com.tangem.Database
import com.tangem.VerificationState

interface CardValuesStorage {

    fun saveValues(cardId: String,
                   isPin1Default: Boolean, isPin2Default: Boolean,
                   cardVerification: VerificationState?,
                   cardValidation: VerificationState?,
                   codeVerification: VerificationState?)

    fun getValues(cardId: String): CardValues?

}

class CardValuesDbStorage(driver: SqlDriver) : CardValuesStorage {

    private val cardValuesQueries: CardValuesEntityQueries

    init {
        val database = Database(driver, cardValuesAdapter = CardValues.Adapter(
                cardVerificationAdapter = EnumColumnAdapter(),
                cardValidationAdapter = EnumColumnAdapter(),
                codeVerificationAdapter = EnumColumnAdapter()
        ))
        cardValuesQueries = database.cardValuesEntityQueries
    }

    override fun saveValues(cardId: String,
                            isPin1Default: Boolean, isPin2Default: Boolean,
                            cardVerification: VerificationState?,
                            cardValidation: VerificationState?,
                            codeVerification: VerificationState?) {
        cardValuesQueries.insertOrReplace(
                cardId,
                isPin1Default, isPin2Default,
                cardVerification, cardValidation, codeVerification
        )
    }

    override fun getValues(cardId: String): CardValues? =
            cardValuesQueries.selectByCardId(cardId).executeAsOneOrNull()

    companion object {
        fun initJvm() = CardValuesDbStorage(
                JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { Database.Schema.create(it) }
        )
    }
}