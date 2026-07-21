package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.terminal.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentTerminalModal(
    amount: Double,
    tableName: String,
    terminalStep: TerminalStep,
    selectedBrand: TerminalBrand,
    onSelectBrand: (TerminalBrand) -> Unit,
    selectedPaymentMethod: PaymentMethod,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    installments: Int,
    onSelectInstallments: (Int) -> Unit,
    onStartPayment: () -> Unit,
    onConfirmPix: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pagamento na Maquininha",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$tableName • Total: R$ ${String.format("%.2f", amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // If Payment in progress or finished, show Smart POS Terminal Frame
                if (terminalStep !is TerminalStep.Idle) {
                    SmartPosHardwareFrame(
                        terminalStep = terminalStep,
                        selectedBrand = selectedBrand,
                        amount = amount,
                        tableName = tableName,
                        onConfirmPix = onConfirmPix,
                        onClose = onClose
                    )
                } else {
                    // Payment Setup Configuration Controls
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // 1. Terminal Brand Selection
                        Text(
                            text = "1. Selecione a Maquininha de Cartão",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TerminalBrand.values().forEach { brand ->
                                val isSelected = (selectedBrand == brand)
                                val brandColor = Color(android.graphics.Color.parseColor(brand.brandColorHex))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) brandColor else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectBrand(brand) }
                                        .testTag("brand_${brand.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = brand.displayName,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = brand.displayName.split(" ").first(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Payment Method Selection
                        Text(
                            text = "2. Forma de Pagamento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentMethod.values().forEach { method ->
                                val isSelected = (selectedPaymentMethod == method)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectPaymentMethod(method) }
                                        .testTag("method_${method.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when (method) {
                                                    PaymentMethod.CREDIT -> Icons.Default.CreditCard
                                                    PaymentMethod.DEBIT -> Icons.Default.Payment
                                                    PaymentMethod.PIX -> Icons.Default.QrCode
                                                    PaymentMethod.VOUCHER -> Icons.Default.CardMembership
                                                    PaymentMethod.CASH -> Icons.Default.AttachMoney
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = method.displayName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onSelectPaymentMethod(method) }
                                        )
                                    }
                                }
                            }
                        }

                        // Installment count selector if CREDIT
                        if (selectedPaymentMethod == PaymentMethod.CREDIT) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Parcelamento:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(1, 2, 3).forEach { count ->
                                        FilterChip(
                                            selected = (installments == count),
                                            onClick = { onSelectInstallments(count) },
                                            label = { Text("${count}x ${if (count == 1) "à vista" else ""}") }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Button
                        Button(
                            onClick = onStartPayment,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(android.graphics.Color.parseColor(selectedBrand.brandColorHex))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("enviar_maquininha_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ENVIAR PARA ${selectedBrand.displayName.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartPosHardwareFrame(
    terminalStep: TerminalStep,
    selectedBrand: TerminalBrand,
    amount: Double,
    tableName: String,
    onConfirmPix: () -> Unit,
    onClose: () -> Unit
) {
    val brandColor = Color(android.graphics.Color.parseColor(selectedBrand.brandColorHex))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Hardware POS Case Body
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1F1D1B),
            tonalElevation = 16.dp,
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
                .border(3.dp, brandColor, RoundedCornerShape(28.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Top Brand Logo & Status Light
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedBrand.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // LED Status Indicator Light
                    val ledColor = when (terminalStep) {
                        is TerminalStep.Approved -> Color(0xFF00C853)
                        is TerminalStep.Failed -> Color.Red
                        else -> Color(0xFFFFB300)
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ledColor)
                    )
                }

                // Interactive LCD Screen Display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0A0A0A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (terminalStep) {
                            is TerminalStep.Connecting -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = brandColor, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = terminalStep.message,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            is TerminalStep.WaitingForCard -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Contactless,
                                        contentDescription = null,
                                        tint = brandColor,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "APROXIME OU INSIRA",
                                        color = brandColor,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "R$ ${String.format("%.2f", amount)}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = terminalStep.method.displayName,
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            is TerminalStep.DisplayingPixQr -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "PIX - LEIA O QR CODE",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "QR Code PIX",
                                        tint = Color.White,
                                        modifier = Modifier.size(110.dp)
                                    )
                                    Text(
                                        text = "R$ ${String.format("%.2f", amount)}",
                                        color = brandColor,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = onConfirmPix,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Simular PIX Pago", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }

                            is TerminalStep.EnterPin -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = brandColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "DIGITE A SENHA",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• • • •",
                                        color = brandColor,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            is TerminalStep.Processing -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = brandColor)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = terminalStep.statusMessage,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            is TerminalStep.Approved -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF00C853),
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "TRANSAÇÃO APROVADA!",
                                        color = Color(0xFF00C853),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "NSU: ${terminalStep.nsu}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "AUT: ${terminalStep.authCode}",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            is TerminalStep.PrintingReceipt -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        tint = brandColor,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "IMPRIMINDO VIA...",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = terminalStep.copyType,
                                        color = brandColor,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }

                // Physical Keypad Simulation
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(16.dp, 6.dp).background(Color.Red, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(16.dp, 6.dp).background(Color.Yellow, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(16.dp, 6.dp).background(Color(0xFF00C853), RoundedCornerShape(2.dp)))
                }
            }
        }

        // Printable Receipt Preview if Printing
        if (terminalStep is TerminalStep.PrintingReceipt) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFFDE7),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(300.dp)
                    .wrapContentHeight()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "COMPROVANTE IMPRESSO (IMPRESSORA TÉRMICA)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    terminalStep.lines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CONCLUIR E FECHAR")
                    }
                }
            }
        }
    }
}
