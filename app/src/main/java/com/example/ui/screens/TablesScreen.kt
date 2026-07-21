package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OrderEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(
    activeOrders: List<OrderEntity>,
    selectedTableName: String,
    onSelectTable: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Standard table list 1 to 12 and comandas 101 to 108
    val tablesAndCommands = remember {
        (1..12).map { "Mesa ${String.format("%02d", it)}" } + (101..108).map { "Comanda $it" } + listOf("Balcão")
    }

    var showSplitBillDialog by remember { mutableStateOf(false) }
    var splitPeopleCount by remember { mutableStateOf(2) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Controle de Mesas e Comandas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Acompanhe em tempo real o status dos atendimentos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Split Bill Action Button
            OutlinedButton(
                onClick = { showSplitBillDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dividir Conta")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatusLegendItem(color = Color(0xFF00C853), label = "Livre")
            StatusLegendItem(color = Color(0xFFD84315), label = "Ocupada")
            StatusLegendItem(color = MaterialTheme.colorScheme.primary, label = "Selecionada")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of Tables & Comandas
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("tables_grid")
        ) {
            items(tablesAndCommands) { name ->
                val activeOrder = activeOrders.find { it.tableOrCommandName == name }
                val isSelected = (selectedTableName == name)
                val isOccupied = (activeOrder != null && activeOrder.subtotal > 0)

                TableCard(
                    name = name,
                    isOccupied = isOccupied,
                    isSelected = isSelected,
                    orderTotal = activeOrder?.total ?: 0.0,
                    itemCount = activeOrder?.customerCount ?: 1,
                    onClick = { onSelectTable(name) }
                )
            }
        }
    }

    // Split Bill Calculator Modal
    if (showSplitBillDialog) {
        val selectedOrder = activeOrders.find { it.tableOrCommandName == selectedTableName }
        val totalToSplit = selectedOrder?.total ?: 0.0

        AlertDialog(
            onDismissRequest = { showSplitBillDialog = false },
            title = { Text("Calculadora de Divisão de Conta - $selectedTableName") },
            text = {
                Column {
                    Text(
                        text = "Total da Mesa: R$ ${String.format("%.2f", totalToSplit)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Número de Pessoas na Mesa:")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        IconButton(
                            onClick = { if (splitPeopleCount > 1) splitPeopleCount-- },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Diminuir")
                        }

                        Text(
                            text = "$splitPeopleCount pessoas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { splitPeopleCount++ },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Aumentar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Valor por Pessoa:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "R$ ${String.format("%.2f", if (splitPeopleCount > 0) totalToSplit / splitPeopleCount else totalToSplit)}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSplitBillDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun StatusLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun TableCard(
    name: String,
    isOccupied: Boolean,
    isSelected: Boolean,
    orderTotal: Double,
    itemCount: Int,
    onClick: () -> Unit
) {
    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isOccupied -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isOccupied -> Color(0xFFD84315)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("table_card_$name")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isOccupied) Color(0xFFD84315) else Color(0xFF00C853))
                )
            }

            if (isOccupied) {
                Column {
                    Text(
                        text = "Ocupada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "R$ ${String.format("%.2f", orderTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = "Livre para atendimento",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
