package com.oneledger.samsung.domain

import com.oneledger.samsung.data.ledger.CreditCardEntity
import kotlin.math.roundToInt

data class RewardCalculation(
    val grossPoints: Double,
    val foreignFee: Double,
    val netValue: Double,
    val effectiveRatePercent: Double,
    val rewardUnit: String,
)

/**
 * 信用卡點數／現金回饋計算器：
 * 支援指定分類加碼、海外消費加碼與跨境 1.5% 國外交易手續費淨回饋試算。
 */
object RewardCalculator {
    fun calculate(
        amount: Double,
        category: String,
        isForeignTx: Boolean,
        card: CreditCardEntity?,
    ): RewardCalculation {
        if (card == null || amount <= 0.0) {
            return RewardCalculation(0.0, 0.0, 0.0, 0.0, "")
        }

        val bonusCats = card.bonusCategoriesCsv
            .split(",", "，")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val appliedRate = when {
            isForeignTx -> card.foreignRatePercent
            bonusCats.contains(category) -> card.bonusRatePercent
            else -> card.baseRatePercent
        }

        val grossPoints = ((amount * appliedRate / 100.0) * 10.0).roundToInt() / 10.0
        val foreignFee = if (isForeignTx) ((amount * card.foreignFeePercent / 100.0) * 10.0).roundToInt() / 10.0 else 0.0
        val netValue = ((grossPoints - foreignFee) * 10.0).roundToInt() / 10.0
        val effectiveRate = if (isForeignTx) appliedRate - card.foreignFeePercent else appliedRate

        return RewardCalculation(
            grossPoints = grossPoints,
            foreignFee = foreignFee,
            netValue = netValue,
            effectiveRatePercent = (effectiveRate * 10.0).roundToInt() / 10.0,
            rewardUnit = card.rewardUnit,
        )
    }
}
