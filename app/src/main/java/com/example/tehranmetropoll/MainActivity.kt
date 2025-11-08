package com.example.tehranmetropoll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tehranmetropoll.navigasion.MetroApp
import com.example.tehranmetropoll.viewModel.MainViewModel
import com.example.tehranmetropoll.viewModel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val context = LocalContext.current
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(context)
            )

            val linearGradiant= Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2962FF),
                    Color(0xFF858585),
                    Color(0xFFFFEB3B)
                ),
                start = Offset(Float.POSITIVE_INFINITY,0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)

            )

            Surface(modifier = Modifier.fillMaxSize()) {

                Box(modifier = Modifier.fillMaxSize().background(linearGradiant)) {

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        MetroApp(viewModel=viewModel)

                    }

                }

            }











    }
}
}

