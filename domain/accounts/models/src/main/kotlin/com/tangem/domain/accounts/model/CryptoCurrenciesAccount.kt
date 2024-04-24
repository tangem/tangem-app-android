package com.tangem.domain.accounts.model

data class CryptoCurrenciesAccount(
    val id: ID,
    val title: Title,
    val currenciesCount: Int,
    val isArchived: Boolean,
) {

    sealed interface ID {

        val value: Int

        data object Main : ID {
            override val value: Int = MAIN_ACCOUNT_ID
        }

        @JvmInline
        value class Default(override val value: Int) : ID {

            init {
                require(value = value != MAIN_ACCOUNT_ID) {
                    "Account ID should not be equal to $MAIN_ACCOUNT_ID"
                }
            }
        }

        companion object {
            const val MAIN_ACCOUNT_ID = 0

            fun fromValue(value: Int): ID {
                return if (value == MAIN_ACCOUNT_ID) {
                    Main
                } else {
                    Default(value)
                }
            }
        }
    }

    sealed interface Title {

        val value: String?

        data object Main : Title {
            override val value: String? = null
        }

        @JvmInline
        value class Default(override val value: String) : Title {

            init {
                require(value = value.isNotBlank()) {
                    "Title should not be blank"
                }

                require(value = value.length <= MAX_LENGTH) {
                    "Title length should be less than or equal to $MAX_LENGTH"
                }
            }
        }

        companion object {
            const val MAX_LENGTH = 15

            fun fromValue(value: String?): Title {
                return if (value == null) {
                    Main
                } else {
                    Default(value)
                }
            }
        }
    }
}