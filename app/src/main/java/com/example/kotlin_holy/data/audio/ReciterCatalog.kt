package com.example.kotlin_holy.data.audio

import com.example.kotlin_holy.domain.model.Reciter

/**
 * Sozlamalarda tanlanadigan qorilar.
 *
 * Ro'yxat qo'lda emas, tekshiruv orqali tuzilgan: har bir qorining audio fayli
 * haqiqatan yuklanishi va so'z-vaqt segmentlari mavjudligi sinab ko'rilgan.
 * Segmentlari yo'q yoki fayli ochilmaydigan qorilar ataylab kiritilmagan —
 * aks holda so'zma-so'z yonish ishlamay qoladi.
 */
object ReciterCatalog {

    val all: List<Reciter> = listOf(
        Reciter(7, "Mishari al-Afasiy", "Murattal"),
        Reciter(2, "Abdulbosit Abdussamad", "Murattal"),
        Reciter(1, "Abdulbosit Abdussamad", "Mujawwad"),
        Reciter(3, "Abdurrahmon as-Sudays", "Murattal"),
        Reciter(4, "Abu Bakr ash-Shotiriy", "Murattal"),
        Reciter(5, "Hani ar-Rifoiy", "Murattal"),
        Reciter(6, "Xalil al-Husariy", "Murattal"),
        Reciter(12, "Xalil al-Husariy", "Muallim"),
        Reciter(9, "Siddiq al-Minshawiy", "Murattal"),
        Reciter(10, "Saud ash-Shuraym", "Murattal"),
    )

    val default: Reciter = all.first()

    fun byId(id: Int): Reciter = all.firstOrNull { it.id == id } ?: default
}
