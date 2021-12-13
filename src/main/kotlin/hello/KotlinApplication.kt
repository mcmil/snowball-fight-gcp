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
            ServerResponse.ok().body(Mono.just("Let the battle begin!"))
        }

        POST("/**", accept(APPLICATION_JSON)) { request ->
            request.bodyToMono(ArenaUpdate::class.java).flatMap { arenaUpdate ->
                println(arenaUpdate)
                val myUrl = arenaUpdate._links.self.href
                val myself = arenaUpdate.arena.state[myUrl]!!



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


                ServerResponse.ok().body(if(!shouldThrow.isEmpty() && !myself.wasHit) {
                    Mono.just("T")
                } else{
                    Mono.just(listOf("F", "R", "L").random())
                })
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
