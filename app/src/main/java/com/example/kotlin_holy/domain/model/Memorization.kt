package com.example.kotlin_holy.domain.model

import androidx.compose.runtime.Immutable

/** Oyat yodlandi deb belgilanishi uchun ketma-ket kerak bo'lgan to'g'ri o'qishlar soni */
const val MEMORIZATION_TARGET = 10

/** Bitta oyatning yodlash jarayonidagi holati */
@Immutable
data class MemorizationProgress(
    val verseKey: String,
    /** Hozirgi ketma-ket to'g'ri o'qishlar soni (xato o'qilsa 0 ga qaytadi) */
    val correctStreak: Int = 0,
    val memorizedAt: Long? = null,
) {
    val isMemorized: Boolean get() = memorizedAt != null
}
