package it.trotta.ticketonbus.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import it.trotta.ticketonbus.BuildConfig
import it.trotta.ticketonbus.data.GoogleAccount
import it.trotta.ticketonbus.data.googleAccountOf

class GoogleSignInException(
    val kind: Kind,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Kind { NOT_CONFIGURED, CANCELLED, NO_ACCOUNT, FAILED }
}

object GoogleSignIn {

    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()

    suspend fun signIn(context: Context): GoogleAccount {
        if (!isConfigured) throw GoogleSignInException(GoogleSignInException.Kind.NOT_CONFIGURED)

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val credential = try {
            CredentialManager.create(context).getCredential(context, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw GoogleSignInException(GoogleSignInException.Kind.CANCELLED, cause = e)
        } catch (e: NoCredentialException) {
            throw GoogleSignInException(GoogleSignInException.Kind.NO_ACCOUNT, cause = e)
        } catch (e: GetCredentialException) {
            throw GoogleSignInException(GoogleSignInException.Kind.FAILED, e.message, e)
        } catch (e: Exception) {
            throw GoogleSignInException(GoogleSignInException.Kind.FAILED, e.message, e)
        }

        val isGoogleId = credential is CustomCredential &&
            (
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
                )
        if (!isGoogleId) throw GoogleSignInException(GoogleSignInException.Kind.FAILED)

        val idToken = GoogleIdTokenCredential.createFrom((credential as CustomCredential).data)
        return googleAccountOf(
            id = idToken.id,
            email = idToken.email,
            displayName = idToken.displayName,
            givenName = idToken.givenName,
            picture = idToken.profilePictureUri,
        ) ?: throw GoogleSignInException(GoogleSignInException.Kind.FAILED)
    }
}
