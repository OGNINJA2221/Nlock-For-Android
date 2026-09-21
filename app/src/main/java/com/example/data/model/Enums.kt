package com.example.data.model

enum class AppCategory(val displayName: String) {
    ALL("All"),
    SOCIAL("Social"),
    MESSAGING("Messaging"),
    FINANCE("Banking & Finance"),
    MEDIA("Photos & Media"),
    SYSTEM("System"),
    OTHER("General")
}

enum class LockType(val displayName: String) {
    PIN("PIN Code"),
    PATTERN("Pattern Lock"),
    KNOCK("Knock Lock")
}

enum class PinLength(val length: Int, val displayName: String) {
    FOUR(4, "4 Digits"),
    SIX(6, "6 Digits"),
    EIGHT(8, "8 Digits")
}

enum class KnockLayout(val displayName: String, val zoneCount: Int) {
    GRID_2X2_CENTER("2x2 + Center", 5),
    GRID_3X3("3x3", 9)
}

enum class KnockLength(val length: Int, val displayName: String) {
    FOUR(4, "4 Taps"),
    SIX(6, "6 Taps"),
    EIGHT(8, "8 Taps")
}

enum class AppThemeMode {
    DEFAULT_GLASS_BLUE,
    DARK_GLASS,
    PURE_WHITE_GLASS,
    AUTO
}
