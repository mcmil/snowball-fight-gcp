package hello

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

@SpringBootApplication
class KotlinApplication {

    @Bean
    fun routes() = router {
        GET {
            ServerResponse.ok().body(Mono.just("Let the battle begin! From GH"))
        }

        POST("/**", accept(APPLICATION_JSON)) { request ->
            request.bodyToMono(ArenaUpdate::class.java).flatMap { arenaUpdate ->
                println(arenaUpdate)
                val myUrl = arenaUpdate._links.self.href
                val myself = arenaUpdate.arena.state[myUrl]!!
                val (sizeX, sizeY) = arenaUpdate.arena.dims


                val shouldThrow = arenaUpdate.arena.state.filter { (href, state) ->
                    val xdiff = myself.x - state.x
                    val ydiff = myself.y - state.y

                    when (myself.direction) {
                        "N" -> xdiff == 0 && ydiff > 0 && ydiff <= 3
                        "S" -> xdiff == 0 && ydiff < 0 && ydiff >= -3
                        "W" -> ydiff == 0 && xdiff > 0 && xdiff <= 3
                        else -> ydiff == 0 && xdiff < 0 && xdiff >= -3
                    } && href != myUrl
                }

                ServerResponse.ok().body(
                    if (!shouldThrow.isEmpty() && !myself.wasHit) {
                        Mono.just("T")
                    } else {
                        when {
                            myself.x == sizeX - 1 && myself.direction == "E" -> Mono.just("R")
                            myself.x == sizeX - 1 && myself.direction == "N" -> Mono.just("L")
                            myself.x == 0 && myself.direction == "W" -> Mono.just("R")
                            myself.x == 0 && myself.direction == "N" -> Mono.just("R")
                            myself.y == 0 && myself.direction == "E" -> Mono.just("R")
                            myself.y == 0 && myself.direction == "W" -> Mono.just("L")
                            myself.y == sizeY - 1 && myself.direction == "S" -> Mono.just("R")
                            myself.y == sizeY - 1 && myself.direction == "W" -> Mono.just("R")
                            myself.y == sizeY - 1 && myself.direction == "E" -> Mono.just("L")
                            else -> Mono.just(listOf("F", "F", "R", "L").random())
                        }
                    }
                )
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<KotlinApplication>(*args)
}

data class ArenaUpdate(val _links: Links, val arena: Arena)
data class PlayerState(val x: Int, val y: Int, val direction: String, val score: Int, val wasHit: Boolean)
data class Links(val self: Self)
data class Self(val href: String)
data class Arena(val dims: List<Int>, val state: Map<String, PlayerState>)
