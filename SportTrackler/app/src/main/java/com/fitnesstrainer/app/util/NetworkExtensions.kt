package com.fitnesstrainer.app.util

import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.isNetworkError(): Boolean =
    this is UnknownHostException ||
    this is ConnectException ||
    this is SocketTimeoutException ||
    (this is IOException && this !is JsonSyntaxException)

fun Throwable.toUserMessage(): String = when {
    this is UnknownHostException ||
    this is ConnectException      -> "Нет подключения к интернету"
    this is SocketTimeoutException -> "Превышено время ожидания"
    this is IOException            -> "Ошибка соединения"
    this is JsonSyntaxException    -> "Ошибка формата ответа сервера"
    else                           -> "Ошибка: ${message ?: javaClass.simpleName}"
}
