package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CashierShiftEntity
import com.example.data.local.PaymentTransactionEntity
import com.example.util.toBRL
import com.example.util.toCentsOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    currentShift: CashierShiftEntity?,
    allTransactions: List<PaymentTransactionEntity>,
    onOpenShift: (Long) -> Unit,
    onCloseShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var floatInput by remember { mutableStateOf("200,00") }

    val isShiftOpen = (currentShift?.status == "OPEN")
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val totalCredit = currentShift?.totalCreditCents ?: 0L
    val totalDebit = currentShift?.totalDebitCents ?: 0L
    val totalPix = currentShift?.totalPixCents ?: 0L
    val totalCash = currentShift?.totalCashCents ?: 0L
    val totalVoucher = currentShift?.totalVoucherCents ?: 0L
    val grandTotalSales = totalCredit + totalDebit + totalPix + totalCash + totalVoucher

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Caixa e Fechamento de Turno",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isShiftOpen) "Turno iniciado em ${currentShift?.openedAt?.let { dateFormat.format(Date(it)) }}" else "Caixa atualmente fechado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isShiftOpen) {
                Button(
                    onClick = onCloseShift,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FECHAR CAIXA")
                }
            } else {
                Button(
                    onClick = { showOpenShiftDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ABRIR CAIXA")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sales Summary Dashboard Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CashierSummaryCard(
                title = "Total Vendas",
                amountCents = grandTotalSales,
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            CashierSummaryCard(
                title = "PIX",
                amountCents = totalPix,
                icon = Icons.Default.QrCode,
                color = Color(0xFF00C853),
                modifier = Modifier.weight(1f)
            )

            CashierSummaryCard(
                title = "Crédito / Débito",
                amountCents = totalCredit + totalDebit,
                icon = Icons.Default.CreditCard,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Histórico de Transações do Dia",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (allTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma transação registrada ainda hoje",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("transactions_list")
            ) {
                items(allTransactions, key = { it.id }) { tx ->
                    ElevatedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        imageVector = when (tx.paymentMethod) {
                                            "PIX" -> Icons.Default.QrCode
                                            "CREDIT", "DEBIT" -> Icons.Default.CreditCard
                                            "CASH" -> Icons.Default.AttachMoney
                                            else -> Icons.Default.Payment
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${tx.paymentMethod} • NSU: ${tx.nsu}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${dateFormat.format(Date(tx.timestamp))} • ${tx.terminalType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = tx.amountCents.toBRL(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Open Shift Dialog
    if (showOpenShiftDialog) {
        AlertDialog(
            onDismissRequest = { showOpenShiftDialog = false },
            title = { Text("Abertura de Caixa") },
            text = {
                Column {
                    Text("Informe o valor de fundo de caixa inicial em dinheiro:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = floatInput,
                        onValueChange = { floatInput = it },
                        label = { Text("Fundo de Caixa (R$)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountCents = floatInput.toCentsOrNull() ?: 0L
                        onOpenShift(amountCents)
                        showOpenShiftDialog = false
                    }
                ) {
                    Text("Confirmar Abertura")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenShiftDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CashierSummaryCard(
    title: String,
    amountCents: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amountCents.toBRL(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}
