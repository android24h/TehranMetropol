package com.example.tehranmetropoll.data.repository

import android.content.Context
import com.example.tehranmetropoll.data.source.MetroNetworkParser
import com.example.tehranmetropoll.domain.model.MetroNetwork
import com.example.tehranmetropoll.domain.repository.MetroRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MetroRepositoryImpl(private val context: Context) : MetroRepository {
    private val mutex = Mutex()
    @Volatile private var cached: MetroNetwork? = null

    override suspend fun getNetwork(): MetroNetwork = cached ?: mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            val json = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            MetroNetworkParser.parse(json)
        }.also { cached = it }
    }

    private companion object {
        const val ASSET = "metro_network.json"
    }
}
