package com.example.domain

import java.security.MessageDigest

object SecurityUtils {

    fun isValidDriverId(driverId: String): Boolean =
        driverId.length == 7 && driverId.all { it.isDigit() }

    fun isValidPassword(password: String): Boolean =
        password.length == 6 && password.all { it.isDigit() }

    fun normalizeMobile(mobile: String): String =
        mobile.filter { it.isDigit() }

    fun isValidMobile(mobile: String): Boolean =
        normalizeMobile(mobile).length >= 7

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
