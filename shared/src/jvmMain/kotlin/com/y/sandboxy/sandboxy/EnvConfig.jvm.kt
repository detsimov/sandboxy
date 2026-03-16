package com.y.sandboxy.sandboxy

import io.github.cdimascio.dotenv.Dotenv

private val dotenv: Dotenv? = runCatching {
    Dotenv.configure().ignoreIfMissing().load()
}.getOrNull()

actual fun getEnvVariable(name: String): String? {
    return dotenv?.get(name) ?: System.getenv(name)
}
