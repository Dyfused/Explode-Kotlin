package explode.backend.ktor

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.execution.ValueUnboxer
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

@JvmInline
value class NNInt(val value: Int)

object CustomSchemaGeneratorHooks : SchemaGeneratorHooks {

	override fun willGenerateGraphQLType(type: KType): GraphQLType? = when(type.classifier as? KClass<*>) {
		NNInt::class -> ExtendedScalars.NonNegativeInt
		UUID::class -> ExtendedScalars.UUID
		LocalDate::class -> ExtendedScalars.Date
		OffsetDateTime::class -> ExtendedScalars.DateTime
		else -> null
	}

	object NNIntUnboxer : ValueUnboxer {
		override fun unbox(boxedValue: Any?): Any? =
			if(boxedValue is NNInt) {
				boxedValue.value
			} else {
				boxedValue
			}
	}
}