package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.model.AccountType
import com.example.model.BackgroundStyle
import com.example.ui.theme.LocalHisabColors

@Composable
fun HisabAtmosphericBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val extra = LocalHisabColors.current
    val baseBg = MaterialTheme.colorScheme.background
    val bgModifier = when (extra.backgroundStyle) {
        BackgroundStyle.FLAT -> Modifier.background(baseBg)
        BackgroundStyle.GRADIENT -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    baseBg,
                    extra.preset.heroGradientStart.copy(alpha = if (extra.isDark) 0.34f else 0.09f),
                    baseBg
                )
            )
        )
        BackgroundStyle.AURORA_GLOW -> Modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    extra.accentColor.copy(alpha = if (extra.isDark) 0.28f else 0.14f),
                    extra.preset.secondary.copy(alpha = if (extra.isDark) 0.16f else 0.08f),
                    baseBg
                ),
                center = Offset(220f, 160f),
                radius = 1350f
            )
        )
    }

    val orbBlurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(radius = 52.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .then(bgModifier)
    ) {
        // Soft luminous ambient orbs behind frosted glass surfaces
        if (extra.backgroundStyle != BackgroundStyle.FLAT) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(orbBlurModifier)
            ) {
                val orbAlphaPrimary = if (extra.isDark) 0.28f else 0.16f
                val orbAlphaSecondary = if (extra.isDark) 0.22f else 0.13f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extra.accentColor.copy(alpha = orbAlphaPrimary),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.18f, size.height * 0.14f),
                        radius = size.minDimension * 0.65f
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.14f),
                    radius = size.minDimension * 0.65f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extra.preset.secondary.copy(alpha = orbAlphaSecondary),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.42f),
                        radius = size.minDimension * 0.58f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.42f),
                    radius = size.minDimension * 0.58f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extra.preset.tertiary.copy(alpha = orbAlphaSecondary * 0.8f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.30f, size.height * 0.86f),
                        radius = size.minDimension * 0.55f
                    ),
                    center = Offset(size.width * 0.30f, size.height * 0.86f),
                    radius = size.minDimension * 0.55f
                )
            }
        }

        content()
    }
}

fun resolveCategoryIcon(iconKey: String): ImageVector = when (iconKey) {
    "restaurant" -> Icons.Rounded.Restaurant
    "directions_car" -> Icons.Rounded.DirectionsCar
    "receipt_long" -> Icons.Rounded.ReceiptLong
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "medical_services" -> Icons.Rounded.MedicalServices
    "school" -> Icons.Rounded.School
    "payments" -> Icons.Rounded.Payments
    "storefront" -> Icons.Rounded.Storefront
    "laptop" -> Icons.Rounded.Laptop
    else -> Icons.Rounded.Category
}

fun resolveAccountIcon(accountType: String): ImageVector = when (AccountType.fromWire(accountType)) {
    AccountType.CASH -> Icons.Rounded.AccountBalanceWallet
    AccountType.BANK -> Icons.Rounded.AccountBalance
    AccountType.WALLET -> Icons.Rounded.PhoneAndroid
    AccountType.SAVINGS -> Icons.Rounded.Savings
}
