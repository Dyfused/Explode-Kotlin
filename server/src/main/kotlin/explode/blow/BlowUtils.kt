package explode.blow

import com.expediagroup.graphql.generator.TopLevelObject
import explode.blow.impl.BlowMutationServiceImpl
import explode.blow.impl.BlowQueryServiceImpl
import explode.dataprovider.provider.IBlowAccessor
import graphql.schema.DataFetchingEnvironment

object BlowUtils {

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

	internal val IBlowAccessor.query: TopLevelObject get() = TopLevelObject(BlowQueryServiceImpl(this))
	internal val IBlowAccessor.mutation: TopLevelObject get() = TopLevelObject(BlowMutationServiceImpl(this))

}