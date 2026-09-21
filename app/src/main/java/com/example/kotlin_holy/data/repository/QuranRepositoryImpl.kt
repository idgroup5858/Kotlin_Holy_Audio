package com.example.kotlin_holy.data.repository

import com.example.kotlin_holy.data.local.AssetJsonSource
import com.example.kotlin_holy.data.mapper.toDomain
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.DataMeta
import com.example.kotlin_holy.domain.model.DifficultWord
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.PageContent
import com.example.kotlin_holy.domain.model.PageSummary
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.domain.model.SurahDetail
import com.example.kotlin_holy.domain.repository.QuranRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranRepositoryImpl @Inject constructor(
    private val source: AssetJsonSource,
) : QuranRepository {

    override suspend fun surahs(): List<Surah> = source.surahs().map { it.toDomain() }

    override suspend fun surah(number: Int): SurahDetail = source.surah(number).toDomain()

    override suspend fun pages(): List<PageSummary> = source.pages().map { it.toDomain() }

    override suspend fun page(number: Int): PageContent = source.page(number).toDomain()

    override suspend fun mushafPage(number: Int): MushafPage = source.mushafPage(number).toDomain()

    override suspend fun juzAyahs(number: Int): List<Ayah> =
        source.juz(number).map { it.toDomain() }

    override suspend fun words(): List<DifficultWord> = source.words().items.map { it.toDomain() }

    override suspend fun meta(): DataMeta? = runCatching { source.meta().toDomain() }.getOrNull()
}
