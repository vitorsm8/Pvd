package com.example.util

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Todo valor monetário no PDV Gourmet é armazenado e calculado em
 * CENTAVOS (Long) — nunca em Double.
 *
 * Por quê: Double não representa exatamente a maioria dos valores
 * decimais (ex: 0.1 + 0.2 != 0.3), e esses pequenos erros se acumulam
 * em somas de pedidos, turnos de caixa e relatórios. Long é exato para
 * inteiros e não tem esse problema.
 *
 * Convenção: qualquer campo/variável que represente dinheiro termina em
 * "Cents" (ex: priceCents, totalCents) para deixar a unidade óbvia e
 * evitar que alguém some sem querer um valor em reais com um em centavos.
 *
 * Double só deve aparecer em dinheiro em dois lugares, e ambos são
 * intencionais e seguros: (1) o instante de formatação para exibição
 * (conversão única, não acumulada, não persistida) e (2) o cálculo de
 * porcentagem (ex: taxa de serviço), que arredonda de volta para Long
 * imediatamente, sem deixar erro se propagar.
 */

private val BRL_LOCALE = Locale("pt", "BR")

/** Formata centavos como "R$ 12,50". */
fun Long.toBRL(): String = String.format(BRL_LOCALE, "R$ %.2f", this / 100.0)

/**
 * Formata um valor em reais (Double) como "R$ 12,50".
 * Use apenas para cálculos de exibição que não são persistidos nem
 * reutilizados em outras contas, como o valor por pessoa ao dividir uma
 * conta (que pode ter fração de centavo e não faz sentido arredondar
 * antes de mostrar).
 */
fun Double.toBRL(): String = String.format(BRL_LOCALE, "R$ %.2f", this)

/**
 * Calcula uma porcentagem sobre um valor em centavos, arredondando para
 * o centavo mais próximo. Ex: 1000L.percentOf(10.0) == 100L (R$ 10,00
 * de R$ 100,00).
 */
fun Long.percentOf(percent: Double): Long = (this * percent / 100.0).roundToLong()

/**
 * Converte texto digitado pelo usuário (ex: "12,50", "12.50") para
 * centavos. Retorna null se o texto não for um valor válido ou for
 * negativo. Aceita tanto vírgula quanto ponto como separador decimal;
 * não lida com separador de milhar (não é esperado nos campos deste
 * app, que são preço de item de cardápio e fundo de caixa).
 */
fun String.toCentsOrNull(): Long? {
    val normalized = trim().replace(",", ".")
    val reais = normalized.toDoubleOrNull() ?: return null
    if (reais < 0) return null
    return (reais * 100).roundToLong()
}
