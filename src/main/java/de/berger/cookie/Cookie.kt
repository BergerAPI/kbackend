package de.berger.cookie

data class Cookie(
    val name: String,
    val value: String,
    val maxAge: Long = -1L,
    val expires: String,
    val path: String,
    val domain: String,
    val secure: Boolean,
    val httpOnly: Boolean
) {

    /**
     * Returns a string representation of the cookie.
     */
    fun getAttributes(): String = mutableListOf<String>().apply {
        add("$name=$value")
        add("Value=$value")
        if (maxAge != -1L) add("Max-Age=$maxAge")
        if (expires.isNotEmpty()) add("Expires=$expires")
        if (path.isNotEmpty()) add("Path=$path")
        if (domain.isNotEmpty()) add("Domain=$domain")
        if (secure) add("Secure")
        if (httpOnly) add("HttpOnly")
    }.joinToString("; ")

    companion object {

        /**
         * Creates a cookie with default values.
         */
        fun createCookie(
            name: String,
            value: String,
            maxAge: Long = -1L,
            expires: String = "",
            path: String = "/",
            domain: String = "",
            secure: Boolean = false,
            httpOnly: Boolean = false
        ): Pair<String, String> =
            "Set-Cookie" to Cookie(name, value, maxAge, expires, path, domain, secure, httpOnly).getAttributes()

    }

}