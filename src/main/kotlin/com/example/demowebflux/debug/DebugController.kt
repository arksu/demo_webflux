package com.example.demowebflux.debug

import com.example.demowebflux.repo.AccountRepo
import com.example.demowebflux.util.LoggerDelegate
import com.example.jooq.Tables.ACCOUNT
import com.example.jooq.tables.records.AccountRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.time.Duration
import java.util.*

@RestController
@RequestMapping("debug")
class DebugController(
    private val dslContext: DSLContext,
) {
    val log by LoggerDelegate()

    val webClient = WebClient.builder()
        .baseUrl("https://jsonplaceholder.typicode.com/posts")
        .build()

    data class Post(
        val id: Int,
        val userId: Int,
        val title: String,
        val body: String,
    )

    @GetMapping("delay")
    fun delayed(): Mono<List<Post>> {
        val mono = webClient
            .get()
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<Post>>() {})
            .publishOn(Schedulers.parallel())

        val mono2 = mono.flatMap {
            Flux.concat(it.map { post ->
                saveBlogPostToMono(post)
//                    .delayElement(Duration.ofMillis(100))
                    .then(Mono.just(post))
            }).collectList()
        }
        return mono2
    }

    @GetMapping("delay2")
    suspend fun delayed2(): List<Post> {
        val mono = webClient
            .get()
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<Post>>() {})
            .publishOn(Schedulers.parallel())

        val list = mono.awaitSingle()
        list.map {
            saveBlogPostToMono(it).awaitSingle()
//            delay(100)
        }
        return list
    }

    private fun saveBlogPostToMono(post: Post): Mono<AccountRecord> {
        // DEBUG CODE for test
        return dslContext.insertInto(ACCOUNT)
            .set(ACCOUNT.NAME, post.title)
            .set(ACCOUNT.DESCRIPTION, post.body)
            .returning()
            .toMono()
            .map {
//                log.debug("saved")
                it
            }
    }
}