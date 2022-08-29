package de.alxgrk.plugins

import de.alxgrk.authentication.AllowedAuthenticationTypes
import de.alxgrk.authentication.userId
import de.alxgrk.data.Address
import de.alxgrk.data.PaymentInfo
import de.alxgrk.data.UserRepository
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import org.kodein.di.instance
import java.util.*

fun Application.configureRouting() {

    val userRepository: UserRepository by di.instance()

    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.Unauthorized, cause.toErrorMessage())
        }
        exception<AuthorizationException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.Forbidden, cause.toErrorMessage())
        }
        exception<NotFoundException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.NotFound, cause.toErrorMessage())
        }
        exception<IllegalArgumentException> { call, cause ->
            call.application.log.error(cause)
            call.respond(HttpStatusCode.BadRequest, ErrorMessage("Bad Request"))
        }
    }

    routing {
        authenticate(AllowedAuthenticationTypes.AdminAuth.key) {
            get<Users> {
                val users = userRepository.findAll()
                call.respond(users)
            }
        }
        authenticate(AllowedAuthenticationTypes.JWTAuth.key) {
            get<Users.ById> { user ->
                val foundUser = userRepository.findById(UUID.fromString(user.id)) ?: throw NotFoundException()
                call.respond(foundUser)
            }
            get<Users.Me> {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val user = userRepository.findById(userId) ?: throw NotFoundException()
                call.respond(user)
            }
            post<Users.Me.Address> {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val address = call.receive<Address>()

                val user = userRepository.updateAddress(userId, address) ?: throw NotFoundException()
                call.respond(user)
            }
            post<Users.Me.PaymentInfo> {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.userId

                val paymentInfo = call.receive<PaymentInfo>()

                val user = userRepository.updatePaymentInfo(userId, paymentInfo) ?: throw NotFoundException()
                call.respond(user)
            }
        }
    }
}

@Serializable
@Resource("/users")
private class Users {
    @Serializable
    @Resource("/{id}")
    class ById(val parent: Users = Users(), val id: String)

    @Serializable
    @Resource("/me")
    class Me(val parent: Users = Users()) {

        @Serializable
        @Resource("/address")
        class Address(val parent: Me = Me()) {}

        @Serializable
        @Resource("/paymentInfo")
        class PaymentInfo(val parent: Me = Me()) {}
    }
}

@Serializable
class ErrorMessage(val message: String)
class AuthenticationException(message: String? = null) : RuntimeException(message ?: "")
class AuthorizationException(message: String) : RuntimeException(message)

fun Exception.toErrorMessage() = message?.let { ErrorMessage(it) } ?: emptyMap<String, String>()
