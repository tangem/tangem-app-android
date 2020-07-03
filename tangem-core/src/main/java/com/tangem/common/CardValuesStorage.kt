package com.tangem.common

import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.tangem.CardValues
import com.tangem.CardValuesEntityQueries
import com.tangem.Database
import com.tangem.SessionEnvironment

interface CardValuesStorage {

    fun saveValues(environment: SessionEnvironment)

    fun getValues(cardId: String) : CardValues?

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

    override fun saveValues(environment: SessionEnvironment) {
        environment.card?.cardId?.let {cardId ->
            cardValuesQueries.insertOrReplace(
                    cardId,
                    environment.pin1?.isDefault ?: false,
                    environment.pin2?.isDefault ?: false,
                    environment.cardVerification, environment.cardVerification,
                    environment.codeVerification
            )
        }
    }

    override fun getValues(cardId: String): CardValues?  =
            cardValuesQueries.selectByCardId(cardId).executeAsOneOrNull()

}