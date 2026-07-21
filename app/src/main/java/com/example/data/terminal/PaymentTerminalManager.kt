package com.example.data.terminal

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

enum class TerminalBrand(val displayName: String, val brandColorHex: String, val protocolName: String) {
    STONE("Stone POS", "#00A868", "Stone SDK / Ton Link"),
    PAGBANK("PagBank / Moderninha", "#FFC107", "PagSeguro Smart Protocol"),
    CIELO("Cielo Lio", "#00529C", "Cielo Lio Orders API"),
    REDE("Rede Smart", "#E02B20", "e-Rede SmartPOS"),
    GENERIC_SMART("Maquininha Universal Wi-Fi/BT", "#673AB7", "SmartPOS TCP/IP Direct")
}

enum class PaymentMethod(val displayName: String) {
    CREDIT("Cartão de Crédito"),
    DEBIT("Cartão de Débito"),
    PIX("PIX QR Code"),
    VOUCHER("Vale Refeição (VR/Alelo/Sodexo)"),
    CASH("Dinheiro")
}

sealed class TerminalStep {
    object Idle : TerminalStep()
    data class Connecting(val brand: TerminalBrand, val message: String) : TerminalStep()
    data class WaitingForCard(val brand: TerminalBrand, val amount: Double, val method: PaymentMethod) : TerminalStep()
    data class DisplayingPixQr(val amount: Double, val qrCodePayload: String) : TerminalStep()
    data class EnterPin(val brand: TerminalBrand) : TerminalStep()
    data class Processing(val brand: TerminalBrand, val statusMessage: String) : TerminalStep()
    data class Approved(
        val transactionId: String,
        val nsu: String,
        val authCode: String,
        val cardBrand: String,
        val amount: Double,
        val paymentMethod: PaymentMethod
    ) : TerminalStep()
    data class PrintingReceipt(val copyType: String, val lines: List<String>) : TerminalStep()
    data class Failed(val reason: String) : TerminalStep()
}

class PaymentTerminalManager {

    private val _currentStep = MutableStateFlow<TerminalStep>(TerminalStep.Idle)
    val currentStep: Flow<TerminalStep> = _currentStep.asStateFlow()

    private val _selectedTerminal = MutableStateFlow(TerminalBrand.STONE)
    val selectedTerminal = _selectedTerminal.asStateFlow()

    fun selectTerminalBrand(brand: TerminalBrand) {
        _selectedTerminal.value = brand
    }

    suspend fun startPayment(
        amount: Double,
        method: PaymentMethod,
        installments: Int = 1,
        tableOrCommandName: String = "Mesa"
    ): PaymentResult {
        val brand = _selectedTerminal.value

        // Step 1: Connecting to POS Terminal
        _currentStep.value = TerminalStep.Connecting(
            brand,
            "Conectando ao terminal ${brand.displayName} via ${brand.protocolName}..."
        )
        delay(900)

        // Step 2: Handle PIX vs Card
        if (method == PaymentMethod.PIX) {
            val pixPayload = "00020126580014br.gov.bcb.pix0136${UUID.randomUUID()}5204000053039865405${String.format("%.2f", amount)}5802BR5915PDV_GOURMET6009SAO_PAULO62070503***6304"
            _currentStep.value = TerminalStep.DisplayingPixQr(amount, pixPayload)
            
            // Wait for user to confirm PIX payment in simulation screen or timer
            return PaymentResult.PendingPix(pixPayload)
        }

        if (method == PaymentMethod.CASH) {
            // Cash direct completion
            val nsu = Random.nextInt(100000, 999999).toString()
            val authCode = "CASH-" + Random.nextInt(1000, 9999)
            _currentStep.value = TerminalStep.Approved(
                transactionId = UUID.randomUUID().toString(),
                nsu = nsu,
                authCode = authCode,
                cardBrand = "DINHEIRO",
                amount = amount,
                paymentMethod = method
            )
            return PaymentResult.Success(nsu, authCode, "DINHEIRO")
        }

        // Card flow
        _currentStep.value = TerminalStep.WaitingForCard(brand, amount, method)
        delay(1200)

        _currentStep.value = TerminalStep.EnterPin(brand)
        delay(1200)

        _currentStep.value = TerminalStep.Processing(brand, "Comunicando com adquirente ${brand.displayName}...")
        delay(1200)

        // Generate approval details
        val nsu = Random.nextInt(100000000, 999999999).toString()
        val authCode = Random.nextInt(100000, 999999).toString()
        val cardBrands = listOf("VISA", "MASTERCARD", "ELO", "AMEX")
        val chosenBrand = cardBrands.random()

        val approvedStep = TerminalStep.Approved(
            transactionId = UUID.randomUUID().toString(),
            nsu = nsu,
            authCode = authCode,
            cardBrand = chosenBrand,
            amount = amount,
            paymentMethod = method
        )
        _currentStep.value = approvedStep
        delay(1000)

        // Receipt Printing simulation
        val receiptLines = generateReceiptLines(tableOrCommandName, amount, method, installments, chosenBrand, nsu, authCode)
        _currentStep.value = TerminalStep.PrintingReceipt("VIA ESTABELECIMENTO", receiptLines)

        return PaymentResult.Success(nsu, authCode, chosenBrand)
    }

    suspend fun simulatePixConfirm(amount: Double, tableOrCommandName: String): PaymentResult.Success {
        val nsu = Random.nextInt(100000000, 999999999).toString()
        val authCode = "PIX-" + Random.nextInt(100000, 999999)
        _currentStep.value = TerminalStep.Approved(
            transactionId = UUID.randomUUID().toString(),
            nsu = nsu,
            authCode = authCode,
            cardBrand = "PIX",
            amount = amount,
            paymentMethod = PaymentMethod.PIX
        )
        delay(800)

        val receiptLines = generateReceiptLines(tableOrCommandName, amount, PaymentMethod.PIX, 1, "PIX BANCO CENTRAL", nsu, authCode)
        _currentStep.value = TerminalStep.PrintingReceipt("VIA CLIENTE E ESTABELECIMENTO", receiptLines)

        return PaymentResult.Success(nsu, authCode, "PIX")
    }

    fun resetTerminal() {
        _currentStep.value = TerminalStep.Idle
    }

    private fun generateReceiptLines(
        tableName: String,
        amount: Double,
        method: PaymentMethod,
        installments: Int,
        cardBrand: String,
        nsu: String,
        authCode: String
    ): List<String> {
        return listOf(
            "==================================",
            "       RESTAURANTE PDV GOURMET    ",
            "   CNPJ: 12.345.678/0001-90       ",
            "   Rua dos Sabores, 1000 - SP     ",
            "==================================",
            "COMPROVANTE DE PAGAMENTO - $tableName",
            "DATA: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(java.util.Date())}",
            "FORMA PGTO: ${method.displayName}",
            "BANDEIRA: $cardBrand",
            "PARCELAS: ${installments}x",
            "TOTAL: R$ ${String.format("%.2f", amount)}",
            "----------------------------------",
            "NSU: $nsu",
            "COD. AUTORIZACAO: $authCode",
            "STATUS: TRANSAÇÃO APROVADA",
            "MAQUININHA: ${_selectedTerminal.value.displayName}",
            "==================================",
            "      OBRIGADO E VOLTE SEMPRE!    ",
            "=================================="
        )
    }
}

sealed class PaymentResult {
    data class Success(val nsu: String, val authCode: String, val cardBrand: String) : PaymentResult()
    data class PendingPix(val qrPayload: String) : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}
