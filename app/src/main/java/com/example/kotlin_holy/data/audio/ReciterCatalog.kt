package com.example.kotlin_holy.data.audio

import com.example.kotlin_holy.domain.model.Reciter

/**
 * Sozlamalarda tanlanadigan qorilar.
 *
 * Ro'yxat qo'lda emas, tekshiruv orqali tuzilgan (2026-11): har bir qori uchun
 * 114 suraning hammasida
 *   — API vaqt jadvalini qaytaradi va undagi oyatlar soni suradagi oyatlar
 *     soniga teng;
 *   — mp3 fayl haqiqatan mavjud (HEAD so'rovi 200, audio/mpeg).
 * Shartlardan biri bitta surada bajarilmasa ham qori kiritilmagan:
 *   168 (Minshawiy, bolalar bilan) — 33-surada 73 oyatdan faqat 22 tasining vaqti bor;
 *   173 (Afasiy, "streaming") — 7-qorining past sifatli nusxasi.
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
        Reciter(97, "Yosir ad-Dusariy", "Murattal"),
        Reciter(161, "Xalifa at-Tunayjiy", "Murattal"),
    )

    val default: Reciter = all.first()

    fun byId(id: Int): Reciter = all.firstOrNull { it.id == id } ?: default

    /** Saqlangan qori ro'yxatdan chiqarilgan bo'lsa, standart qoriga qaytiladi */
    fun isAvailable(id: Int): Boolean = all.any { it.id == id }
}
