package com.fitnesstrainer.app

import android.app.Application
import android.content.Intent
import com.fitnesstrainer.app.data.local.TokenStorage
import com.fitnesstrainer.app.data.local.db.AppDatabase
import com.fitnesstrainer.app.data.network.ApiService
import com.fitnesstrainer.app.data.network.RetrofitClient
import com.fitnesstrainer.app.ui.MainActivity
import com.fitnesstrainer.app.util.NetworkMonitor

class App : Application() {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var apiService: ApiService
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance       = this
        tokenStorage   = TokenStorage(this)
        apiService     = RetrofitClient.create(tokenStorage)
        networkMonitor = NetworkMonitor(this)
        database       = AppDatabase.getInstance(this)

        RetrofitClient.onSessionExpired = {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("session_expired", true)
            }
            startActivity(intent)
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
