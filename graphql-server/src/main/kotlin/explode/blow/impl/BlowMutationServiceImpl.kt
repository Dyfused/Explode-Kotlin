package explode.blow.impl

import explode.blow.BlowMutationService
import explode.blow.BlowUtils.soudayo
import explode.dataprovider.model.game.*
import explode.dataprovider.provider.IBlowAccessor
import graphql.schema.DataFetchingEnvironment

class BlowMutationServiceImpl(private val p: IBlowAccessor) : BlowMutationService {

	override suspend fun loginUser(env: DataFetchingEnvironment, username: String?, password: String?): UserModel = with(p) {
		p.loginUser(username!!, password!!).tunerize
	}

	override suspend fun registerUser(env: DataFetchingEnvironment, username: String?, password: String?): UserModel = with(p) {
		p.registerUser(username!!, password!!).tunerize
	}

	// Dynamite Obsolete
	override suspend fun purchaseChart(env: DataFetchingEnvironment, chartId: String?): UserModel = with(p) {
		p.getUser("")!!.tunerize // TODO
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