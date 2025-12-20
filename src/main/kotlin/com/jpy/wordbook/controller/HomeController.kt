package com.jpy.wordbook.controller

import com.jpy.wordbook.service.VocabularyService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.views.View

@Controller
class HomeController(
    private val vocabularyService: VocabularyService
) {

    @Get("/")
    @View("home")
    fun index(): Map<String, Any> {
        return mapOf(
            "title" to "Tanoshii",
            "wordCount" to vocabularyService.getWordCount(),
            "categories" to vocabularyService.getAllCategories()
        )
    }
}
