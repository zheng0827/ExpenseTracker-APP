package com.oneledger.samsung.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsTransit
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color,
)

object Categories {
    val expenseList = listOf(
        CategoryInfo("餐飲", Icons.Rounded.Restaurant, Color(0xFF0381FE)),
        CategoryInfo("交通", Icons.Rounded.DirectionsTransit, Color(0xFFF59E0B)),
        CategoryInfo("日常", Icons.Rounded.ShoppingBasket, Color(0xFF148A4B)),
        CategoryInfo("購物", Icons.Rounded.ShoppingBag, Color(0xFF8B5CF6)),
        CategoryInfo("訂閱", Icons.Rounded.Repeat, Color(0xFFEC4899)),
        CategoryInfo("帳單", Icons.Rounded.ReceiptLong, Color(0xFF06B6D4)),
        CategoryInfo("娛樂", Icons.Rounded.Movie, Color(0xFFEF4444)),
        CategoryInfo("醫療", Icons.Rounded.LocalHospital, Color(0xFF10B981)),
        CategoryInfo("其他", Icons.Rounded.Sell, Color(0xFF64748B)),
    )

    val incomeList = listOf(
        CategoryInfo("薪資", Icons.Rounded.Work, Color(0xFF148A4B)),
        CategoryInfo("獎金", Icons.Rounded.WorkspacePremium, Color(0xFFF59E0B)),
        CategoryInfo("投資", Icons.Rounded.TrendingUp, Color(0xFF0381FE)),
        CategoryInfo("回饋", Icons.Rounded.CardGiftcard, Color(0xFFEC4899)),
        CategoryInfo("其他收入", Icons.Rounded.AttachMoney, Color(0xFF64748B)),
    )

    val paymentMethods = listOf("現金", "信用卡", "交通卡", "行動支付", "銀行轉帳")

    private val lookup: Map<String, CategoryInfo> =
        (expenseList + incomeList).associateBy { it.name }

    fun find(name: String): CategoryInfo =
        lookup[name] ?: CategoryInfo(name, Icons.Rounded.AccountBalanceWallet, Color(0xFF64748B))
}
