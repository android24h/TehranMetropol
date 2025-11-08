package com.example.tehranmetropoll.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.data.model.StationDataModel
import com.example.tehranmetropoll.ui.theme.getLineColor
import com.example.tehranmetropoll.viewModel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    origin: String,
    destination: String,
    onValueChangeOrigin: (String) -> Unit,
    onValueChangeDestination: (String) -> Unit,
    onSelectOrigin: (StationDataModel) -> Unit,
    onSelectDestination: (StationDataModel) -> Unit,
    originSuggestions: List<StationDataModel>,
    destinationSuggestions: List<StationDataModel>,
    onSearch: () -> Unit,
    route: List<StationDataModel>,
    onStationClick: (StationDataModel) -> Unit,
    viewModel: MainViewModel,
    navController: NavHostController
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.acd),
            contentDescription = "",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {



            Text(text = "نرم افزار مسیریابی مـتروپل",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    color = Color.White,
                    shadow = Shadow(
                        offset = Offset(1f,3f),
                        color = Color(0xFFFFC107)
                    )))
            Spacer(modifier = Modifier.height(12.dp))


            // 🔹 AutoComplete مبدأ
            var originExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = originExpanded,
                onExpandedChange = { originExpanded = it }
            ) {
                OutlinedTextField(
                    value = origin,
                    onValueChange = {
                        onValueChangeOrigin(it)
                        originExpanded = true
                    },
                    label = { Text("ایستگاه مبدأ") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFFD600),   // رنگ خط وقتی انتخاب شده
                        unfocusedBorderColor = Color.White,
                        focusedLabelColor = Color.Yellow,
                        unfocusedLabelColor = Color.White // رنگ لیبل وقتی غیرفعال// رنگ خط وقتی انتخاب نشده
                    ),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = originExpanded && originSuggestions.isNotEmpty(),
                    onDismissRequest = { originExpanded = false }
                ) {
                    originSuggestions.forEach { station ->
                        DropdownMenuItem(
                            text = { Text(station.nameFa.ifBlank { station.name }) },
                            onClick = {
                                onSelectOrigin(station)
                                originExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹* AutoComplete مقصد
            var destExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = destExpanded,
                onExpandedChange = { destExpanded = it }
            ) {
                OutlinedTextField(
                    value = destination,
                    onValueChange = {
                        onValueChangeDestination(it)
                        destExpanded = true
                    },
                    label = { Text("ایستگاه مقصد") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFFD600),   // رنگ خط وقتی انتخاب شده
                        unfocusedBorderColor = Color.White,
                        focusedLabelColor = Color.Yellow,   // رنگ لیبل وقتی فعال
                        unfocusedLabelColor = Color.White // رنگ لیبل وقتی غیرفعال// رنگ خط وقتی انتخاب نشده
                    ),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = destExpanded && destinationSuggestions.isNotEmpty(),
                    onDismissRequest = { destExpanded = false }
                ) {
                    destinationSuggestions.forEach { station ->
                        DropdownMenuItem(
                            text = { Text("${station.nameFa.ifBlank { { station.name } }} (${station.name})") },
                            onClick = {
                                onSelectDestination(station)
                                destExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.findRouteWithTransfer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("یافتن مسیر بین خطوط", fontSize = 18.sp)
            }


            Spacer(modifier = Modifier.height(16.dp))



            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 نمایش مسیر
            AnimatedVisibility(
                visible = route.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Text(
                        text = "مسیر پیشنهادی:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp), color = Color.Yellow
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(route) { station ->
                            val isTransfer = route.count { it.name == station.name } > 1

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isTransfer)
                                        Color(0xFFFFA726)
                                    else
                                        getLineColor(station.line)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onStationClick(station) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${station.name} (خط ${station.line})",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }


                        }

                        item {
                            Button(
                                onClick = {
                                    navController.navigate("map")
                                }, modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {

                                Text(text = "مشاهده مسیر روی نقشه")

                            }
                        }


                    }

                }
            }

            if (route.isEmpty()) {
                Text(
                    "مسیر یافت نشد!",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }


        }

    }




}




