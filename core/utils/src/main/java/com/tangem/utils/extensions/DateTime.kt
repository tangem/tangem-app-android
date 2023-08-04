package com.tangem.utils.extensions

import org.joda.time.DateTime
import org.joda.time.LocalDate

fun DateTime.isToday(): Boolean = LocalDate.now().equals(LocalDate(this))

fun DateTime.isYesterday(): Boolean = LocalDate.now().minusDays(1).equals(LocalDate(this))