package explode.blow.graphql

import com.expediagroup.graphql.generator.TopLevelObject
import explode.blow.graphql.internal.BlowMutationServiceImpl
import explode.blow.graphql.internal.BlowQueryServiceImpl
import explode.blow.provider.IBlowDataProvider
import graphql.schema.DataFetchingEnvironment

object BlowService {

	/**
	 * Get the Token of the request client.
	 *
	 * In normal case, 'trash-potato-server' is defined as offline token, and no
	 * server requests will be sent. Thus, we use this as the default value,
	 * to distinguish if it is the bad request.
	 */
	internal val DataFetchingEnvironment.soudayo: String
		get() = when(val token = this.graphQlContext.get<String>("soudayo")) {
			"trash-potato-server" -> ""
			else -> token
		}

	internal val IBlowDataProvider.query: TopLevelObject get() = TopLevelObject(BlowQueryServiceImpl(this))
	internal val IBlowDataProvider.mutation: TopLevelObject get() = TopLevelObject(BlowMutationServiceImpl(this))

}