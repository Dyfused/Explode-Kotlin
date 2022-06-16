package explode.schema

import com.expediagroup.graphql.server.operations.Query
import java.time.Instant

class HelloQueryService: Query {

	fun hello() = "${Instant.now()}!"
}