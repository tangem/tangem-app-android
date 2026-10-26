package com.tangem.common.constants

data class TangemPayBankRecipient(
    val displayName: String,
    val firstName: String,
    val lastName: String,
    val businessName: String? = null,
    val accountNumber: String,
    val routingNumber: String,
    val bankName: String,
    val country: String = "United States",
    val region: String = "California",
    val city: String = "Los Angeles",
    val addressLine1: String = "1 Main Street",
    val postalCode: String = "90001",
    val bankRegion: String = "New York",
    val bankCity: String = "New York",
    val bankAddressLine1: String = "270 Park Avenue",
    val bankPostalCode: String = "10017",
) {
    val fullName: String get() = businessName ?: "$firstName $lastName"
    val shortName: String get() = businessName ?: "$firstName ${lastName.first()}."
    val initial: String get() = fullName.take(1)
}

object TangemPayBankAccounts {

    val JOHN = TangemPayBankRecipient(
        displayName = "Rent payments",
        firstName = "John",
        lastName = "Doe",
        accountNumber = "123456789012",
        routingNumber = "021000021",
        bankName = "Chase Bank",
    )

    val ACME = TangemPayBankRecipient(
        displayName = "Supplier invoices",
        firstName = "",
        lastName = "",
        businessName = "Acme Corp",
        accountNumber = "987654321098",
        routingNumber = "026009593",
        bankName = "Bank of America",
    )

    val ALICE = TangemPayBankRecipient(
        displayName = "Family savings",
        firstName = "Alice",
        lastName = "Smith",
        accountNumber = "555500001111",
        routingNumber = "021000021",
        bankName = "Chase Bank",
    )

    val BOB = TangemPayBankRecipient(
        displayName = "Bob personal",
        firstName = "Bob",
        lastName = "Brown",
        accountNumber = "444400002222",
        routingNumber = "121000248",
        bankName = "Wells Fargo",
    )

    val CAROL = TangemPayBankRecipient(
        displayName = "Carol personal",
        firstName = "Carol",
        lastName = "White",
        accountNumber = "333300003333",
        routingNumber = "121000248",
        bankName = "Wells Fargo",
    )

    val JANE = TangemPayBankRecipient(
        displayName = "Jane Roe",
        firstName = "Jane",
        lastName = "Roe",
        accountNumber = "246813579024",
        routingNumber = "021000021",
        bankName = "Chase Bank",
    )

    val MICHAEL = TangemPayBankRecipient(
        displayName = "Michael Scott",
        firstName = "Michael",
        lastName = "Scott",
        accountNumber = "9876543210",
        routingNumber = "026009593",
        bankName = "Bank of America",
        postalCode = "32501",
    )

    val EMILY = TangemPayBankRecipient(
        displayName = "Emily Clark",
        firstName = "Emily",
        lastName = "Clark",
        accountNumber = "8765432109",
        routingNumber = "021000021",
        bankName = "JPMorgan Chase",
        postalCode = "32501",
    )

    val DUNDER = TangemPayBankRecipient(
        displayName = "Dunder Mifflin",
        firstName = "",
        lastName = "",
        businessName = "Dunder Mifflin",
        accountNumber = "7654321098",
        routingNumber = "121000248",
        bankName = "Wells Fargo Bank",
        postalCode = "32501",
    )
}