package com.fitnesstrainer.app.util

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.isNetworkError(): Boolean =
    this is UnknownHostException ||
    this is ConnectException ||
    this is SocketTimeoutException

fun Throwable.toUserMessage(): String =
    if (isNetworkError()) "Нет подключения к интернету" else "Ошибка сервера"
