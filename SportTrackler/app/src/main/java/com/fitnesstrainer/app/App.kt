package com.fitnesstrainer.app

import android.app.Application
import com.fitnesstrainer.app.data.local.TokenStorage
import com.fitnesstrainer.app.data.network.ApiService
import com.fitnesstrainer.app.data.network.RetrofitClient

class App : Application() {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var apiService: ApiService
        private set

    override fun onCreate() {
        super.onCreate()
        instance     = this
        tokenStorage = TokenStorage(this)
        apiService   = RetrofitClient.create(tokenStorage)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
