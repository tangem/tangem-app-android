package com.tangem.data.moneyaccount.contribution

import javax.inject.Qualifier

/** The balance contributors the Money account takes, chosen in `MoneyAccountDataModule`; the portfolio takes them all. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class MoneyAccountContributions