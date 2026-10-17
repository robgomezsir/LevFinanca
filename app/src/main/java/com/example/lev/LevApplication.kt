package com.example.lev

import android.app.Application
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LevApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        // Recoloca a periódica em segundo plano se houver conexão + automático.
        appScope.launch {
            runCatching { container.syncRepository.ensurePeriodicSync() }
        }
    }

    // Init manual do WorkManager (o auto-init via androidx.startup está
    // removido no manifesto). Sem dados do usuário nos logs do worker.
    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.WARN)
                .build()
}
