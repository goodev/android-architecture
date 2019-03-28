@file:Suppress("unused")

package com.example.android.architecture.blueprints.todoapp.data.source

sealed class Result<out T : Any> {

    class Success<out T : Any>(val data: T) : Result<T>()

    class Error(val exception: Throwable) : Result<Nothing>()
}