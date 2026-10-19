package com.example.tehranmetropoll.data.repository

import android.content.Context
import android.util.Log
import com.example.tehranmetropoll.data.source.OperationsParser
import com.example.tehranmetropoll.domain.model.MetroOperations
import com.example.tehranmetropoll.domain.repository.OperationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class OperationsRepositoryImpl(private val context: Context) : OperationsRepository {
    private val mutex = Mutex()
    @Volatile private var cached: MetroOperations? = null

    override suspend fun getOperations(): MetroOperations = cached ?: mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            // A hand-edited file with a typo must not break routing: fall back to "no data".
            runCatching {
                OperationsParser.parse(context.assets.open(ASSET).bufferedReader().use { it.readText() })
            }.onFailure { Log.e(TAG, "Could not read $ASSET", it) }
                .getOrDefault(MetroOperations.EMPTY)
        }.also { cached = it }
    }

    private companion object {
        const val ASSET = "metro_operations.json"
        const val TAG = "OperationsRepository"
    }
}
