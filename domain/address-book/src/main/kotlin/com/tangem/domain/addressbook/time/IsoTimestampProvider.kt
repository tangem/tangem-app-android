package com.tangem.domain.addressbook.time

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

interface IsoTimestampProvider {

    fun now(): String
}

class DefaultIsoTimestampProvider : IsoTimestampProvider {

    override fun now(): String = DateTime.now(DateTimeZone.UTC).toString()
}