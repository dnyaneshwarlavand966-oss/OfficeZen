// OfficeZen 22 official Indian languages + English registry
package com.example.localization

data class IndianLanguage(
  val code: String,
  val nativeName: String,
  val englishName: String,
  val isRtl: Boolean = false,
)

object IndianLanguages {
  // English + All 22 Eighth Schedule Official Indian Languages
  val all: List<IndianLanguage> = listOf(
    IndianLanguage("en", "English", "English"),
    IndianLanguage("hi", "हिन्दी", "Hindi"),
    IndianLanguage("bn", "বাংলা", "Bengali"),
    IndianLanguage("te", "తెలుగు", "Telugu"),
    IndianLanguage("mr", "मराठी", "Marathi"),
    IndianLanguage("ta", "தமிழ்", "Tamil"),
    IndianLanguage("ur", "اردو", "Urdu", isRtl = true),
    IndianLanguage("gu", "ગુજરાતી", "Gujarati"),
    IndianLanguage("kn", "ಕನ್ನಡ", "Kannada"),
    IndianLanguage("ml", "മലയാളം", "Malayalam"),
    IndianLanguage("or", "ଓଡ଼ିଆ", "Odia"),
    IndianLanguage("pa", "ਪੰਜਾਬੀ", "Punjabi"),
    IndianLanguage("as", "অসমীয়া", "Assamese"),
    IndianLanguage("mai", "मैथिली", "Maithili"),
    IndianLanguage("sat", "ᱥᱟᱱᱛᱟᱲᱤ", "Santali"),
    IndianLanguage("ks", "کٲشُر", "Kashmiri", isRtl = true),
    IndianLanguage("ne", "नेपाली", "Nepali"),
    IndianLanguage("sd", "سنڌي", "Sindhi", isRtl = true),
    IndianLanguage("kok", "कोंकणी", "Konkani"),
    IndianLanguage("doi", "डोगरी", "Dogri"),
    IndianLanguage("mni", "ꯃꯩꯇꯩꯂꯣꯟ", "Manipuri"),
    IndianLanguage("brx", "बड़ो", "Bodo"),
    IndianLanguage("sa", "संस्कृतम्", "Sanskrit"),
  )

  fun findByCode(code: String): IndianLanguage =
    all.firstOrNull { it.code == code } ?: all.first()
}
