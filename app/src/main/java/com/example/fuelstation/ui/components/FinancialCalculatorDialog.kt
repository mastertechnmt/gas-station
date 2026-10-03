package com.example.fuelstation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun FinancialCalculatorDialog(
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirmResult: (String) -> Unit
) {
    var display by remember { mutableStateOf(initialValue.ifBlank { "0" }) }
    var expression by remember { mutableStateOf("") }
    var pendingOp by remember { mutableStateOf<Char?>(null) }
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var startNewNumber by remember { mutableStateOf(false) }

    fun evaluate(): Double? {
        val second = display.toDoubleOrNull() ?: return null
        val first = firstOperand ?: return second
        val op = pendingOp ?: return second

        val res = when (op) {
            '+' -> first + second
            '-' -> first - second
            '×', '*' -> first * second
            '÷', '/' -> if (second != 0.0) first / second else 0.0
            else -> second
        }
        return res
    }

    fun formatDouble(d: Double): String {
        return if (d % 1.0 == 0.0) {
            d.toLong().toString()
        } else {
            String.format(java.util.Locale.ENGLISH, "%.3f", d).trimEnd('0').trimEnd('.')
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🧮 آلة حاسبة مالية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display screen
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = expression.ifBlank { " " },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                        Text(
                            text = display,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            maxLines = 1,
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Keypad grid
                val buttons = listOf(
                    listOf("C", "⌫", "%", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", "00", ".", "=")
                )

                buttons.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { btn ->
                            val isOp = btn in listOf("÷", "×", "-", "+")
                            val isAction = btn in listOf("C", "⌫", "%")
                            val isEquals = btn == "="

                            val bgColor = when {
                                isEquals -> Primary
                                isOp -> Primary.copy(alpha = 0.15f)
                                isAction -> Color(0xFFF1F5F9)
                                else -> SurfaceWhite
                            }
                            val txtColor = when {
                                isEquals -> Color.White
                                isOp -> Primary
                                isAction -> TextSecondary
                                else -> TextPrimary
                            }

                            Button(
                                onClick = {
                                    when (btn) {
                                        "C" -> {
                                            display = "0"
                                            expression = ""
                                            firstOperand = null
                                            pendingOp = null
                                            startNewNumber = false
                                        }
                                        "⌫" -> {
                                            display = if (display.length > 1) display.dropLast(1) else "0"
                                        }
                                        "%" -> {
                                            val v = (display.toDoubleOrNull() ?: 0.0) / 100.0
                                            display = formatDouble(v)
                                        }
                                        "+", "-", "×", "÷" -> {
                                            val currentVal = display.toDoubleOrNull() ?: 0.0
                                            if (firstOperand != null && pendingOp != null && !startNewNumber) {
                                                val res = evaluate() ?: currentVal
                                                firstOperand = res
                                                display = formatDouble(res)
                                            } else {
                                                firstOperand = currentVal
                                            }
                                            pendingOp = btn[0]
                                            expression = "${formatDouble(firstOperand!!)} $btn"
                                            startNewNumber = true
                                        }
                                        "=" -> {
                                            if (firstOperand != null && pendingOp != null) {
                                                val res = evaluate()
                                                if (res != null) {
                                                    val resStr = formatDouble(res)
                                                    onConfirmResult(resStr)
                                                    onDismiss()
                                                }
                                            } else {
                                                onConfirmResult(display)
                                                onDismiss()
                                            }
                                        }
                                        "." -> {
                                            if (startNewNumber) {
                                                display = "0."
                                                startNewNumber = false
                                            } else if (!display.contains(".")) {
                                                display += "."
                                            }
                                        }
                                        else -> { // Digits
                                            if (startNewNumber || display == "0") {
                                                display = btn
                                                startNewNumber = false
                                            } else {
                                                display += btn
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = txtColor),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                if (btn == "⌫") {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "مسح",
                                        modifier = Modifier.size(20.dp),
                                        tint = txtColor
                                    )
                                } else {
                                    Text(
                                        text = btn,
                                        fontSize = if (btn == "=" || isOp) 20.sp else 18.sp,
                                        fontWeight = if (isEquals || isOp) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
