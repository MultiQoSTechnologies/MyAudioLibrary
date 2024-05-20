package com.example.myaudiolibrary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myaudiolibrary.R

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

val fontSemiBold by lazy {
    TextStyle(
        fontFamily = FontFamily(Font(R.font.urbanist_semibold)),
        fontWeight = FontWeight(600),
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = Color.Black
    )
}

val fontRegular by lazy {
    TextStyle(
        fontFamily = FontFamily(Font(R.font.urbanist_regular)),
        fontWeight = FontWeight(400),
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = Color.Black
    )
}

val fontMedium by lazy {
    TextStyle(
        fontFamily = FontFamily(Font(R.font.urbanist_medium)),
        fontWeight = FontWeight(500),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Color.Black
    )
}