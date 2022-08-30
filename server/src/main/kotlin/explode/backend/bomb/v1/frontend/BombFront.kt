package explode.backend.bomb.v1.frontend

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.bombFrontendModule() {

	routing {
		authenticate {
			static {
				staticBasePackage = "bombfront"
				resources(".")
				defaultResource("index.html")
			}
		}
	}

}