package dynamite

import AppVersion
import Endpoint
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

val c = OkHttpClient()
val j = ObjectMapper()

inline fun <reified T : Any> SingleQuery.execute(token: String? = null): T {
	return j.readValue(callQuery(this, token), T::class.java)
}

fun SingleQuery.execute(token: String? = null): String = callQuery(this, token)

fun callQuery(query: SingleQuery, token: String? = null): String {
	val r = Request.Builder().post(query.toPostJson().toRequestBody()).url(Endpoint)
		.addHeader("X-VERSION", AppVersion)
		.addHeader("Content-Type", "application/json")
		.addHeader("X-VERIFIER", "FIXME: WHAT THE FUCK IS THE VERIFY") // FIXME: What's the fuck is the X-Verifier?
	token?.let { r.addHeader("X-SOUDAYO", token) }

	val rsp = c.newCall(r.build()).execute()

	return rsp.body?.string() ?: error("Unable to fetch data. Request: $r")
}

private fun encryptSHA384(str: String): String? {
	return try {
		val md = MessageDigest.getInstance("SHA-384")
		val messageDigest = md.digest(str.toByteArray())
		val no = BigInteger(1, messageDigest)
		var hashText = no.toString(16)
		while(hashText.length < 32) {
			hashText = "0$hashText"
		}
		hashText
	} catch(e: NoSuchAlgorithmException) {
		throw IllegalStateException("SHA-384 is not provided by VM.", e)
	}
}