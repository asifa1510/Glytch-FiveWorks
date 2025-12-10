package com.example.glytch

data class Contact(
    var id: String? = null,
    var name: String? = null,
    var phone: String? = null,
    var email: String? = null
) {
    // Convenience constructor
    constructor(name: String, phone: String, email: String) : this(
        id = null,
        name = name,
        phone = phone,
        email = email
    )
}
