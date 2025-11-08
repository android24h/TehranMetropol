package com.example.tehranmetropoll.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.tehranmetropoll.data.db.DatabaseProvider
import com.example.tehranmetropoll.data.model.StationDataModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailScreen(
    stationId: Int,
    onBack: () -> Unit
) {


    val context = LocalContext.current
    val dao = remember { DatabaseProvider.getInstance(context).stationDao() }

    // State برای نگهداری اطلاعات ایستگاه
    var station by remember { mutableStateOf<StationDataModel?>(null) }

    // لود ایستگاه از دیتابیس (در نخ IO)
    LaunchedEffect(stationId) {
        withContext(Dispatchers.IO) {
            val result = dao.getSearchById(stationId)
            station = result
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ایستگاه ${station?.nameFa ?:  "Station Details"}", color = Color(
                        0xFFFFFFFF
                    )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2196F3))
            )
        },
        containerColor = Color.Transparent

        ) { padding ->
        if (station == null) {
            // اگر هنوز لود نشده
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                // اگر ایستگاه لود شد
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("🆔 ID: ${station!!.id}")
                    Text("🚇 Line: ${station!!.line}")
                    Text("📍 Coordinates: ${station!!.x}, ${station!!.y}")
                    Text("🔗 Neighbors: ${station!!.neighbors.joinToString(", ")}")
                }
            }

        }
    }




}