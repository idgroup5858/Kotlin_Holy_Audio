package com.example.kotlin_holy.domain.model

/**
 * Madina mushafi bo'yicha juz chegaralari va har bir suraning bet oralig'i.
 * Qur'on o'zgarmagani uchun bu jadvallar doimiy — ular ma'lumot fayllaridan
 * hisoblab chiqarilgan va shu yerda saqlanadi.
 */
object JuzCatalog {

    val all: List<JuzInfo> = listOf(
        JuzInfo(1, 1, 1, 2, 141, 1, 21, 148),
        JuzInfo(2, 2, 142, 2, 252, 22, 41, 111),
        JuzInfo(3, 2, 253, 3, 92, 42, 62, 126),
        JuzInfo(4, 3, 93, 4, 23, 62, 81, 131),
        JuzInfo(5, 4, 24, 4, 147, 82, 101, 124),
        JuzInfo(6, 4, 148, 5, 81, 102, 121, 110),
        JuzInfo(7, 5, 82, 6, 110, 121, 141, 149),
        JuzInfo(8, 6, 111, 7, 87, 142, 161, 142),
        JuzInfo(9, 7, 88, 8, 40, 162, 181, 159),
        JuzInfo(10, 8, 41, 9, 92, 182, 201, 127),
        JuzInfo(11, 9, 93, 11, 5, 201, 221, 151),
        JuzInfo(12, 11, 6, 12, 52, 222, 241, 170),
        JuzInfo(13, 12, 53, 14, 52, 242, 261, 154),
        JuzInfo(14, 15, 1, 16, 128, 262, 281, 227),
        JuzInfo(15, 17, 1, 18, 74, 282, 301, 185),
        JuzInfo(16, 18, 75, 20, 135, 302, 321, 269),
        JuzInfo(17, 21, 1, 22, 78, 322, 341, 190),
        JuzInfo(18, 23, 1, 25, 20, 342, 361, 202),
        JuzInfo(19, 25, 21, 27, 55, 362, 381, 339),
        JuzInfo(20, 27, 56, 29, 45, 382, 401, 171),
        JuzInfo(21, 29, 46, 33, 30, 402, 421, 178),
        JuzInfo(22, 33, 31, 36, 27, 422, 441, 169),
        JuzInfo(23, 36, 28, 39, 31, 442, 461, 357),
        JuzInfo(24, 39, 32, 41, 46, 462, 481, 175),
        JuzInfo(25, 41, 47, 45, 37, 482, 502, 246),
        JuzInfo(26, 46, 1, 51, 30, 502, 521, 195),
        JuzInfo(27, 51, 31, 57, 29, 522, 541, 399),
        JuzInfo(28, 58, 1, 66, 12, 542, 561, 137),
        JuzInfo(29, 67, 1, 77, 50, 562, 581, 431),
        JuzInfo(30, 78, 1, 114, 6, 582, 604, 564),
    )

    fun byNumber(number: Int): JuzInfo? = all.getOrNull(number - 1)

    /** Har bir sura egallagan betlar: [birinchi, oxirgi], 1-sura 0-indeksda */
    private val surahPages: Array<IntArray> = arrayOf(
        intArrayOf(1, 1), intArrayOf(2, 49), intArrayOf(50, 76), intArrayOf(77, 106),
        intArrayOf(106, 127), intArrayOf(128, 150), intArrayOf(151, 176), intArrayOf(177, 186),
        intArrayOf(187, 207), intArrayOf(208, 221), intArrayOf(221, 235), intArrayOf(235, 248),
        intArrayOf(249, 255), intArrayOf(255, 261), intArrayOf(262, 267), intArrayOf(267, 281),
        intArrayOf(282, 293), intArrayOf(293, 304), intArrayOf(305, 312), intArrayOf(312, 321),
        intArrayOf(322, 331), intArrayOf(332, 341), intArrayOf(342, 349), intArrayOf(350, 359),
        intArrayOf(359, 366), intArrayOf(367, 376), intArrayOf(377, 385), intArrayOf(385, 396),
        intArrayOf(396, 404), intArrayOf(404, 410), intArrayOf(411, 414), intArrayOf(415, 417),
        intArrayOf(418, 427), intArrayOf(428, 434), intArrayOf(434, 440), intArrayOf(440, 445),
        intArrayOf(446, 452), intArrayOf(453, 458), intArrayOf(458, 467), intArrayOf(467, 476),
        intArrayOf(477, 482), intArrayOf(483, 489), intArrayOf(489, 495), intArrayOf(496, 498),
        intArrayOf(499, 502), intArrayOf(502, 506), intArrayOf(507, 510), intArrayOf(511, 515),
        intArrayOf(515, 517), intArrayOf(518, 520), intArrayOf(520, 523), intArrayOf(523, 525),
        intArrayOf(526, 528), intArrayOf(528, 531), intArrayOf(531, 534), intArrayOf(534, 537),
        intArrayOf(537, 541), intArrayOf(542, 545), intArrayOf(545, 548), intArrayOf(549, 551),
        intArrayOf(551, 552), intArrayOf(553, 554), intArrayOf(554, 555), intArrayOf(556, 557),
        intArrayOf(558, 559), intArrayOf(560, 561), intArrayOf(562, 564), intArrayOf(564, 566),
        intArrayOf(566, 568), intArrayOf(568, 570), intArrayOf(570, 571), intArrayOf(572, 573),
        intArrayOf(574, 575), intArrayOf(575, 577), intArrayOf(577, 578), intArrayOf(578, 580),
        intArrayOf(580, 581), intArrayOf(582, 583), intArrayOf(583, 584), intArrayOf(585, 585),
        intArrayOf(586, 586), intArrayOf(587, 587), intArrayOf(587, 589), intArrayOf(589, 589),
        intArrayOf(590, 590), intArrayOf(591, 591), intArrayOf(591, 592), intArrayOf(592, 592),
        intArrayOf(593, 594), intArrayOf(594, 594), intArrayOf(595, 595), intArrayOf(595, 596),
        intArrayOf(596, 596), intArrayOf(596, 596), intArrayOf(597, 597), intArrayOf(597, 597),
        intArrayOf(598, 598), intArrayOf(598, 599), intArrayOf(599, 599), intArrayOf(599, 600),
        intArrayOf(600, 600), intArrayOf(600, 600), intArrayOf(601, 601), intArrayOf(601, 601),
        intArrayOf(601, 601), intArrayOf(602, 602), intArrayOf(602, 602), intArrayOf(602, 602),
        intArrayOf(603, 603), intArrayOf(603, 603), intArrayOf(603, 603), intArrayOf(604, 604),
        intArrayOf(604, 604), intArrayOf(604, 604),
    )

    const val TOTAL_PAGES = 604
    const val TOTAL_SURAHS = 114

    /** Sura qaysi betlardan boshlanib qaysi betda tugashi */
    fun surahPageRange(surahNumber: Int): IntRange? =
        surahPages.getOrNull(surahNumber - 1)?.let { it[0]..it[1] }
}
