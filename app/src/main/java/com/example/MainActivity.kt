package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BottomTabRow
import com.example.ui.components.TopNavBar
import com.example.ui.screens.*
import com.example.ui.theme.PdvGourmetTheme
import com.example.ui.viewmodel.PosTab
import com.example.ui.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PdvGourmetTheme {
                val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
                val selectedTableName by viewModel.selectedTableName.collectAsStateWithLifecycle()
                val currentOrder by viewModel.currentOrder.collectAsStateWithLifecycle()
                val orderItems by viewModel.currentOrderItems.collectAsStateWithLifecycle()
                val categories by viewModel.categories.collectAsStateWithLifecycle()
                val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
                val activeOrders by viewModel.activeOrders.collectAsStateWithLifecycle()
                val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
                val currentCashierShift by viewModel.currentCashierShift.collectAsStateWithLifecycle()
                val showPaymentModal by viewModel.showPaymentModal.collectAsStateWithLifecycle()
                val terminalStep by viewModel.terminalStep.collectAsStateWithLifecycle()
                val selectedTerminalBrand by viewModel.selectedTerminalBrand.collectAsStateWithLifecycle()
                val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
                val installmentCount by viewModel.installmentCount.collectAsStateWithLifecycle()
                val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
                val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopNavBar(
                            selectedTableName = selectedTableName,
                            onSelectTableClick = { viewModel.setActiveTab(PosTab.TABLES) },
                            currentShift = currentCashierShift,
                            activeTerminalBrand = selectedTerminalBrand,
                            onTerminalClick = { viewModel.setActiveTab(PosTab.TERMINALS) }
                        )
                    },
                    bottomBar = {
                        BottomTabRow(
                            activeTab = activeTab,
                            onTabSelected = { tab -> viewModel.setActiveTab(tab) }
                        )
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)

                    when (activeTab) {
                        PosTab.PDV -> PdvScreen(
                            categories = categories,
                            selectedCategoryId = selectedCategoryId,
                            onSelectCategory = { id -> viewModel.setSelectedCategory(id) },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                            products = products,
                            currentOrder = currentOrder,
                            orderItems = orderItems,
                            onAddProduct = { prod -> viewModel.addProductToCurrentOrder(prod) },
                            onUpdateQuantity = { item, qty -> viewModel.updateOrderItemQuantity(item, qty) },
                            onToggleTip = { tip -> viewModel.toggleServiceTip(tip) },
                            onPayClick = { viewModel.openPaymentModal() },
                            modifier = contentModifier
                        )

                        PosTab.TABLES -> TablesScreen(
                            activeOrders = activeOrders,
                            selectedTableName = selectedTableName,
                            onSelectTable = { name ->
                                viewModel.selectTableName(name)
                                viewModel.setActiveTab(PosTab.PDV)
                            },
                            modifier = contentModifier
                        )

                        PosTab.PRODUCTS -> ProductsScreen(
                            categories = categories,
                            products = products,
                            onAddProduct = { name, catId, price, desc, code ->
                                viewModel.addNewProduct(name, catId, price, desc, code)
                            },
                            modifier = contentModifier
                        )

                        PosTab.TERMINALS -> TerminalsScreen(
                            activeTerminalBrand = selectedTerminalBrand,
                            onSelectTerminalBrand = { brand -> viewModel.setTerminalBrand(brand) },
                            modifier = contentModifier
                        )

                        PosTab.CASHIER -> CashierScreen(
                            currentShift = currentCashierShift,
                            allTransactions = allTransactions,
                            onOpenShift = { float -> viewModel.openCashierShift(float) },
                            onCloseShift = { viewModel.closeCashierShift() },
                            modifier = contentModifier
                        )

                        PosTab.AI_ASSISTANT -> AiAssistantScreen(
                            currentShift = currentCashierShift,
                            aiResponse = aiResponse,
                            isLoading = isAiLoading,
                            onAskAi = { prompt -> viewModel.askAiAssistant(prompt) },
                            modifier = contentModifier
                        )
                    }

                    // Payment Terminal Interactive Modal
                    if (showPaymentModal) {
                        PaymentTerminalModal(
                            amount = currentOrder?.total ?: 0.0,
                            tableName = selectedTableName,
                            terminalStep = terminalStep,
                            selectedBrand = selectedTerminalBrand,
                            onSelectBrand = { brand -> viewModel.setTerminalBrand(brand) },
                            selectedPaymentMethod = selectedPaymentMethod,
                            onSelectPaymentMethod = { method -> viewModel.setPaymentMethod(method) },
                            installments = installmentCount,
                            onSelectInstallments = { count -> viewModel.setInstallments(count) },
                            onStartPayment = { viewModel.processTerminalPayment() },
                            onConfirmPix = { viewModel.confirmPixPaymentInTerminal() },
                            onClose = { viewModel.closePaymentModal() }
                        )
                    }
                }
            }
        }
    }
}
