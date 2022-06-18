package explode

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.Scalars.*
import graphql.language.Value
import graphql.scalars.ExtendedScalars
import graphql.schema.*
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

@JvmInline
value class NNInt(val i: Int)

object CustomSchemaGeneratorHooks : SchemaGeneratorHooks {

	override fun willGenerateGraphQLType(type: KType): GraphQLType? = when(type.classifier as? KClass<*>) {
		NNInt::class -> ExtendedScalars.NonNegativeInt// CustomNonNegativeIntType
		UUID::class -> ExtendedScalars.UUID
		LocalDate::class -> ExtendedScalars.Date
		else -> null
	}
}

private val CustomNonNegativeIntType = GraphQLScalarType.newScalar()
	.name("NonNegativeInt")
	.description("An Int scalar that must be greater than or equal to zero")
	.coercing(CustomNonNegativeIntCoercing)
	.build()

private object CustomNonNegativeIntCoercing : Coercing<NNInt, Int> {

	private fun Int.toNNINt() = NNInt(this)

	private fun check(i: Int, exceptionMaker: (String) -> RuntimeException): Int {
		if(i < 0) {
			throw exceptionMaker("The value must be greater than or equal to zero")
		}
		return i
	}

	override fun serialize(input: Any): Int {
		val i = GraphQLInt.coercing.serialize(input) as Int
		return check(i, ::CoercingSerializeException)
	}

	override fun parseValue(input: Any): NNInt {
		val i = GraphQLInt.coercing.parseValue(input) as Int
		return check(i, ::CoercingParseValueException).toNNINt()
	}

	override fun parseLiteral(input: Any): NNInt {
		val i = GraphQLInt.coercing.parseLiteral(input) as Int
		return check(i, ::CoercingParseLiteralException).toNNINt()
	}

	override fun valueToLiteral(input: Any): Value<out Value<*>> {
		return GraphQLInt.coercing.valueToLiteral(input)
	}

}