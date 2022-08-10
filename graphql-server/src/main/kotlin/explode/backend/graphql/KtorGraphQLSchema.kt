package explode.backend.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.scalars.IDValueUnboxer
import com.expediagroup.graphql.generator.toSchema
import explode.blow.BlowUtils.mutation
import explode.blow.BlowUtils.query
import explode.dataprovider.provider.BlowException
import explode.dataprovider.provider.IBlowAccessor
import graphql.ExceptionWhileDataFetching
import graphql.GraphQL
import graphql.execution.*
import graphql.language.SourceLocation
import graphql.schema.GraphQLSchema
import java.util.concurrent.CompletionException

private val config = SchemaGeneratorConfig(
	supportedPackages = listOf("explode.blow", "explode.dataprovider.model"),
	hooks = CustomSchemaGeneratorHooks
)

private lateinit var schema: GraphQLSchema

fun getGraphQLSchema(blow: IBlowAccessor): GraphQLSchema {
	if(!::schema.isInitialized) { // lazy init
		schema = toSchema(config, listOf(blow.query), listOf(blow.mutation))
	}

	return schema
}

@Suppress("DEPRECATION")
private val dataFetcherExceptionHandler = object : DataFetcherExceptionHandler {
	@Deprecated("Deprecated in Java")
	override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters): DataFetcherExceptionHandlerResult {
		return if(handlerParameters.exception is BlowException) { // skip the error message
			val exception: Throwable = unwrap(handlerParameters.exception)
			val sourceLocation = handlerParameters.sourceLocation
			val path = handlerParameters.path

			val error = ExplodeExceptionWhileDataFetching(path, exception, sourceLocation)

			DataFetcherExceptionHandlerResult.newResult().error(error).build()
		} else {
			super.onException(handlerParameters)
		}
	}

	protected fun unwrap(exception: Throwable): Throwable {
		if(exception.cause != null) {
			if(exception is CompletionException) {
				return exception.cause!!
			}
		}
		return exception
	}
}

private class ExplodeExceptionWhileDataFetching(
	path: ResultPath,
	exception: Throwable,
	sourceLocation: SourceLocation
) : ExceptionWhileDataFetching(path, exception, sourceLocation) {
	override fun getMessage(): String {
		return this.exception.message ?: super.getMessage()
	}
}

fun getGraphQLObject(blow: IBlowAccessor): GraphQL =
	GraphQL.newGraphQL(getGraphQLSchema(blow))
		.defaultDataFetcherExceptionHandler(dataFetcherExceptionHandler)
		.valueUnboxer(IDValueUnboxer())
		.valueUnboxer(CustomSchemaGeneratorHooks.NNIntUnboxer).build()