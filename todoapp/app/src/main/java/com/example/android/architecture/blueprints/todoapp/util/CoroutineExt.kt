package com.example.android.architecture.blueprints.todoapp.util

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


/**
 * Equivalent to [runBlocking] but return [Unit] instead of [T].
 *
 * Mainly for usage when you want to lift [runBlocking] to return. Example:
 *
 * ```
 * override fun loadData() = runBlockingSilent {
 *     // code
 * }
 * ```
 */
fun <T> runBlockingSilent(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T) {
    runBlocking(context, block)
}