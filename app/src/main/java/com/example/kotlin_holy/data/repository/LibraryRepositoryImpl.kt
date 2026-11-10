package com.example.kotlin_holy.data.repository

import com.example.kotlin_holy.data.local.AssetJsonSource
import com.example.kotlin_holy.domain.model.Book
import com.example.kotlin_holy.domain.model.BookContent
import com.example.kotlin_holy.domain.model.Hadith
import com.example.kotlin_holy.domain.model.HadithCollection
import com.example.kotlin_holy.domain.model.HadithSection
import com.example.kotlin_holy.domain.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val source: AssetJsonSource,
) : LibraryRepository {

    override suspend fun collections(): List<HadithCollection> = source.hadithCollections()

    override suspend fun collection(id: String): HadithCollection? =
        source.hadithCollections().firstOrNull { it.id == id }

    override suspend fun sections(collectionId: String): List<HadithSection> =
        source.hadithSections(collectionId)

    override suspend fun hadiths(collectionId: String, sectionNumber: Int): List<Hadith> =
        source.hadiths(collectionId, sectionNumber)

    override suspend fun books(): List<Book> = source.books()

    override suspend fun book(id: String): Book? = source.books().firstOrNull { it.id == id }

    override suspend fun bookContent(id: String): BookContent = source.bookContent(id)
}
