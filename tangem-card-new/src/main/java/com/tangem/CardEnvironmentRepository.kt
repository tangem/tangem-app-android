package com.tangem

class CardEnvironmentRepository(val dataStorage: DataStorage?) {

    var cardEnvironment: CardEnvironment = initCardEnvironment()
        set(value) {
            if (cardEnvironment != value) {
                saveCardEnvirnoment(value)
            }
        }

    private fun initCardEnvironment(): CardEnvironment {
        return CardEnvironment(
                dataStorage?.getPin1() ?: CardEnvironment.DEFAULT_PIN,
                dataStorage?.getPin2() ?: CardEnvironment.DEFAULT_PIN2
        )
    }

    private fun saveCardEnvirnoment(cardEnvironment: CardEnvironment) {
// [REDACTED_TODO_COMMENT]
    }
}

