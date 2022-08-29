package de.alxgrk

import de.alxgrk.common.AuthType
import de.alxgrk.data.Address
import de.alxgrk.data.PaymentInfo
import de.alxgrk.data.UserDocument
import de.alxgrk.data.UserRepository
import io.ktor.server.netty.*
import de.alxgrk.plugins.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.kodein.di.instance
import java.util.*
import kotlin.concurrent.thread
import kotlin.random.Random

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(Resources)

    configureDiAsync()
    configureSecurity()
    configureSessions()
    configureRouting()
    configureSerialization()
    configureMonitoring()
    configureHTTP()

    dummyData()
}

fun Application.dummyData() {
    thread(isDaemon = true) {
        runBlocking {
            val userRepo: UserRepository by di.instance()

            val user1 = UserDocument(
                id = UUID.fromString("a0925332-2805-4153-ade5-1108aa279e6a"),
                externalId = "123456789",
                login = "user1",
                name = "User 1",
                email = "user1@auction.com",
                authType = AuthType.Github,
                avatarUrl = "http://www.levibio.it/wp-content/uploads/2019/03/female-avatar-profile-icon-round-woman-face-vector-18307274.jpg"
            )
            userRepo.insert(user1)

            val user2 = UserDocument(
                id = UUID.fromString("735db4d6-658d-4ea6-aeed-d0718e076d6a"),
                externalId = "234567891",
                login = "user2",
                name = "User 2",
                email = "user2@auction.com",
                authType = AuthType.Github,
                avatarUrl = "https://cdn2.iconfinder.com/data/icons/avatars-99/62/avatar-366-456318-512.png"
            )
            userRepo.insert(user2)

            val user3 = UserDocument(
                id = UUID.fromString("f76f89f0-aacf-4cb5-ab1a-4b7969364725"),
                externalId = "345678910",
                login = "user3",
                name = "User 3",
                email = "user3@auction.com",
                authType = AuthType.Github,
                avatarUrl = "https://cdn2.iconfinder.com/data/icons/avatars-99/62/avatar-369-456321-512.png"
            )
            userRepo.insert(user3)
        }
    }
}
