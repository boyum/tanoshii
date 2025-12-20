package com.jpy.wordbook.controller

import com.jpy.wordbook.repository.SessionRepository
import com.jpy.wordbook.service.VocabularyService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.views.View

@Controller
class HomeController(
    private val vocabularyService: VocabularyService,
    private val sessionRepository: SessionRepository
) {

    @Get("/")
    @View("home")
    fun index(): Map<String, Any> {
        val recentSessions = sessionRepository.findRecent(10)
        return mapOf(
            "title" to "Tanoshii",
            "wordCount" to vocabularyService.getWordCount(),
            "recentSessions" to recentSessions
        )
    }
}
