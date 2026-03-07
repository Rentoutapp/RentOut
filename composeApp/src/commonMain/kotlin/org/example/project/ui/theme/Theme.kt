package org.example.project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Brand Palette ───────────────────────────────────────────────────────────
object RentOutColors {
    // Primary — Deep Royal Blue (trust, real estate authority)
    val Primary        = Color(0xFF1B4FFF)
    val PrimaryLight   = Color(0xFF5B7FFF)
    val PrimaryDark    = Color(0xFF0030CC)
    val OnPrimary      = Color(0xFFFFFFFF)

    // Secondary — Vibrant Coral/Orange (CTA, energy)
    val Secondary      = Color(0xFFFF6B35)
    val SecondaryLight = Color(0xFFFF9A6C)
    val SecondaryDark  = Color(0xFFCC4A1A)
    val OnSecondary    = Color(0xFFFFFFFF)

    // Tertiary — Emerald Green (verified, success)
    val Tertiary       = Color(0xFF22C55E)
    val TertiaryLight  = Color(0xFF4ADE80)
    val TertiaryDark   = Color(0xFF15803D)
    val OnTertiary     = Color(0xFFFFFFFF)

    // Backgrounds
    val Background     = Color(0xFFF8F9FA)
    val Surface        = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEEF2FF)
    val OnBackground   = Color(0xFF111827)
    val OnSurface      = Color(0xFF1F2937)
    val OnSurfaceVar   = Color(0xFF4B5563)

    // Status Colors
    val StatusPending  = Color(0xFFF59E0B)   // Amber
    val StatusApproved = Color(0xFF22C55E)   // Green
    val StatusRejected = Color(0xFFEF4444)   // Red
    val StatusUnavail  = Color(0xFF9CA3AF)   // Gray

    // Icon tints — subtle, distinct (Rule 1)
    val IconBlue       = Color(0xFF6B8FFF)   // muted royal blue
    val IconTeal       = Color(0xFF2DD4BF)   // muted teal
    val IconAmber      = Color(0xFFFBBF24)   // warm amber
    val IconRose       = Color(0xFFFB7185)   // dusty rose
    val IconPurple     = Color(0xFFA78BFA)   // soft purple
    val IconSlate      = Color(0xFF94A3B8)   // slate blue-gray
    val IconGreen      = Color(0xFF4ADE80)   // soft green
    val IconOrange     = Color(0xFFFB923C)   // soft orange

    // Error
    val Error          = Color(0xFFEF4444)
    val OnError        = Color(0xFFFFFFFF)

    // Dark theme surfaces
    val DarkBackground = Color(0xFF0F172A)
    val DarkSurface    = Color(0xFF1E293B)
    val DarkSurfaceVar = Color(0xFF1B3060)
    val DarkOnSurface  = Color(0xFFE2E8F0)
    val DarkOnBackground = Color(0xFFF1F5F9)
}

// ─── Light Color Scheme ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = RentOutColors.Primary,
    onPrimary        = RentOutColors.OnPrimary,
    primaryContainer = RentOutColors.SurfaceVariant,
    onPrimaryContainer = RentOutColors.PrimaryDark,
    secondary        = RentOutColors.Secondary,
    onSecondary      = RentOutColors.OnSecondary,
    secondaryContainer = Color(0xFFFFE8DC),
    onSecondaryContainer = RentOutColors.SecondaryDark,
    tertiary         = RentOutColors.Tertiary,
    onTertiary       = RentOutColors.OnTertiary,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = RentOutColors.TertiaryDark,
    error            = RentOutColors.Error,
    onError          = RentOutColors.OnError,
    errorContainer   = Color(0xFFFFE4E4),
    background       = RentOutColors.Background,
    onBackground     = RentOutColors.OnBackground,
    surface          = RentOutColors.Surface,
    onSurface        = RentOutColors.OnSurface,
    surfaceVariant   = RentOutColors.SurfaceVariant,
    onSurfaceVariant = RentOutColors.OnSurfaceVar,
    outline          = Color(0xFFD1D5DB),
    outlineVariant   = Color(0xFFE5E7EB),
)

// ─── Dark Color Scheme ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = RentOutColors.PrimaryLight,
    onPrimary        = Color(0xFF001A80),
    primaryContainer = RentOutColors.DarkSurfaceVar,
    onPrimaryContainer = RentOutColors.PrimaryLight,
    secondary        = RentOutColors.SecondaryLight,
    onSecondary      = Color(0xFF7A2800),
    tertiary         = RentOutColors.TertiaryLight,
    onTertiary       = Color(0xFF003916),
    error            = Color(0xFFFF8A8A),
    onError          = Color(0xFF690000),
    background       = RentOutColors.DarkBackground,
    onBackground     = RentOutColors.DarkOnBackground,
    surface          = RentOutColors.DarkSurface,
    onSurface        = RentOutColors.DarkOnSurface,
    surfaceVariant   = Color(0xFF1E3A5F),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline          = Color(0xFF334155),
    outlineVariant   = Color(0xFF1E293B),
)

// ─── Screen Background Colors ─────────────────────────────────────────────────
object RentOutBackgrounds {
    // Light mode backgrounds - subtle tints for better card visibility
    val LightGradientTop = Color(0xFFE8EDF5)      // Subtle blue-gray
    val LightGradientBottom = Color(0xFFF2F4F7)   // Lighter gray-white
    
    // Dark mode uses theme colors directly
}

// ─── Text Colors for Screens ──────────────────────────────────────────────────
object RentOutTextColors {
    // Light mode text colors
    val LightPrimaryText = Color(0xFF1A1F36)      // Dark blue-gray for headers
    val LightSecondaryText = Color(0xFF5B6B8C)    // Medium gray-blue for subtitles
    
    // These adapt to theme automatically
}

// ─── Typography ───────────────────────────────────────────────────────────────
private val RentOutTypography = Typography(
    // Display - Large headlines (e.g., "Who are you?")
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    
    // Title - Section headers and card titles
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    
    // Body - Regular text content
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    
    // Label - Buttons and small UI elements
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp
    ),
    
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    )
)

// ─── Theme Composable ─────────────────────────────────────────────────────────
@Composable
fun RentOutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = RentOutTypography,
        content     = content
    )
}
