package com.example.myaudiolibrary.core

import androidx.compose.ui.graphics.vector.ImageVector
import com.primex.core.Text

interface Route {
    val title: Text
    val icon: ImageVector
    val route: String
}