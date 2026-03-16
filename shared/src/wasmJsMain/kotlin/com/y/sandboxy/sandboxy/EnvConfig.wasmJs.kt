@file:OptIn(ExperimentalWasmJsInterop::class)

package com.y.sandboxy.sandboxy

import kotlin.js.ExperimentalWasmJsInterop

private fun getEnvObject(): JsAny? = js("globalThis.__ENV__")

private fun getProperty(obj: JsAny, key: JsString): JsAny? = js("obj[key]")

actual fun getEnvVariable(name: String): String? {
    val env = getEnvObject() ?: return null
    val value = getProperty(env, name.toJsString()) ?: return null
    return value.toString()
}
