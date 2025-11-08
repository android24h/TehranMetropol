package com.example.tehranmetropoll.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getLineColor(line:Int):Color {
    return when(line){
        1-> Color(0xFFD32F2F)
        2-> Color(0xFF1976D2)
        3-> Color(0xFF388E3C)
        4-> Color(0xFFFBC02D)
        else-> Color(0xFF9E9E9E)

    }

}