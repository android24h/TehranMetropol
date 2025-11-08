package com.example.tehranmetropoll.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder

@Composable
fun TextFeildComponnt(
    value: String,
    onvalueChange: (String) -> Unit,
    label: String

) {
    TextField(
        value = value,
        onValueChange = onvalueChange,
        placeholder = { Text(text = label) },
        modifier = Modifier.fillMaxWidth()
    )
}