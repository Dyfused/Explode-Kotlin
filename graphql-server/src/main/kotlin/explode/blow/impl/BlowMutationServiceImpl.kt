package explode.blow.impl

import explode.blow.BlowMutationService
import explode.blow.BlowUtils.soudayo
import explode.dataprovider.model.*
import explode.dataprovider.provider.IBlowDataProvider
import graphql.schema.DataFetchingEnvironment

class BlowMutationServiceImpl(private val p: IBlowDataProvider) : BlowMutationService {

	override suspend fun loginUser(env: DataFetchingEnvironment, username: String?, password: String?): UserModel {
		return p.loginUser(username!!, password!!)
	}

	override suspend fun registerUser(env: DataFetchingEnvironment, username: String?, password: String?): UserModel {
		return p.registerUser(username!!, password!!)
	}

	// Dynamite Obsolete
	override suspend fun purchaseChart(env: DataFetchingEnvironment, chartId: String?): UserModel {
		return p.getUser("")!! // TODO
	}

	override suspend fun exchangeSet(env: DataFetchingEnvironment, setId: String?): ExchangeSetModel {
		return with(p) {
			p.getUserByToken(env.soudayo)?.buySet(setId!!) ?: error("Invalid Soudayo")
		}
	}

	override suspend fun submitBeforeAssessment(
		env: DataFetchingEnvironment,
		assessmentGroupId: String?,
		medalLevel: Int?
	): BeforePlaySubmitModel {
		return with(p) {
			p.getUserByToken(env.soudayo)?.submitBeforeAssessment(assessmentGroupId!!, medalLevel!!) ?: error("Invalid Soudayo")
		}
	}

	override suspend fun submitAfterAssessment(
		env: DataFetchingEnvironment,
		playRecords: List<PlayRecordInput?>?,
		randomId: String?
	): AfterAssessmentModel {
		return with(p) {
			p.getUserByToken(env.soudayo)?.submitAfterAssessment(playRecords!!.filterNotNull(), randomId!!) ?: error("Invalid Soudayo")
		}
	}

	override suspend fun submitBeforePlay(
		env: DataFetchingEnvironment,
		chartId: String?,
		PPCost: Int?,
		eventArgs: String?
	): BeforePlaySubmitModel {
		return with(p) {
			p.getUserByToken(env.soudayo)?.submitBeforePlay(chartId!!, PPCost!!, eventArgs!!) ?: error("Invalid Soudayo")
		}
	}

	override suspend fun submitAfterPlay(
		env: DataFetchingEnvironment,
		randomId: String?,
		playRecord: PlayRecordInput?
	): AfterPlaySubmitModel {
		return with(p) {
			p.getUserByToken(env.soudayo)?.submitAfterPlay(playRecord!!, randomId!!) ?: error("Invalid Soudayo")
		}
	}
}