package com.mongodb.stitch.android.tutorials.todo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.auth.providers.google.GoogleCredential;

import static com.google.android.gms.auth.api.Auth.GOOGLE_SIGN_IN_API;

public class LogonActivity extends AppCompatActivity {

    private GoogleApiClient _googleApiClient;
    private static final int RC_SIGN_IN = 421;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.logon);
        setupLogin();
    }

    private void setupLogin() {
        setContentView(R.layout.logon);
        enableAnonymousAuth();


        // TODO:
        // 11. Get the google client ID from strings.xml and pass it to your method for setting up
        // Google auth
        // final String googleWebClientId = getString(R.string.google_web_client_id);
        // enableGoogleAuth(googleWebClientId);
    }

    private void enableAnonymousAuth() {
        findViewById(R.id.anon_login_button).setOnClickListener(ignored ->
                TodoListActivity.client.getAuth().loginWithCredential(new AnonymousCredential())
                        .addOnSuccessListener(user -> {
                            Toast.makeText(LogonActivity.this,
                                    "Logged in Anonymously. ID: " + user.getId(),
                                    Toast.LENGTH_LONG).show();
                            setResult(Activity.RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.d("Stitch Auth", "error logging in", e);
                            Toast.makeText(LogonActivity.this, "Failed to log in Anonymously. " +
                                    "Did you enable Anonymous Auth in your Stitch backend and copy " +
                                            "your Stitch App ID to strings.xml?",
                                    Toast.LENGTH_LONG).show();
                        }));
    }


    private void enableGoogleAuth(String googleWebClientId) {

        // TODO:
        // 1. Instantiate a GoogleSignInOptions.Builder object with the default sign-in option
        final GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(googleWebClientId, true);

        // 2. Build it and pass the result to a new GoogleSignInOptions object
        final GoogleSignInOptions gso = gsoBuilder.build();

        // 3. Instantiate the _googleApiClient
        _googleApiClient = new GoogleApiClient.Builder(LogonActivity.this)
                .enableAutoManage(LogonActivity.this, connectionResult ->
                        Log.e("Stitch Auth", "Error connecting to google: " + connectionResult.getErrorMessage()))
                .addApi(GOOGLE_SIGN_IN_API, gso)
                .build();

        // 4. Create an onclick listener for the google_login_button
        findViewById(R.id.google_login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View ignored) {

                // 4a.
                // if we already have a google client, clear it and require the user to log back in.
                // Otherwise, call connect()
                if (_googleApiClient.isConnected()) {
                    _googleApiClient.clearDefaultAccountAndReconnect();
                } else {
                    _googleApiClient.connect();
                }

                // 5. Set sign-in options, create a sign-in client with those options, and initiate
                // the intent
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestServerAuthCode(googleWebClientId)
                        .build();
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(LogonActivity.this, gso);

                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        //TODO
        // 6. Create a GoogleSignInAccount from the task result. Put it in a try-catch
        // and catch any ApiException
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // 7. Create a GoogleCredential from the account.
            final GoogleCredential googleCredential =
                    new GoogleCredential(account.getServerAuthCode());

            // 8. Call getAuth().loginWithCredential on the static TodoListActivity.client object,
            // passing in the googleCredential.
            // If the task is succesful,set the result to Activity.RESULT_OK and end this activity,
            // returning control to the TodoListActivity
            TodoListActivity.client.getAuth().loginWithCredential(googleCredential).addOnCompleteListener(
                    task -> {
                        if (task.isSuccessful()) {
                            setResult(Activity.RESULT_OK);
                            finish();
                        } else {
                            Log.e("Stitch Auth", "Error logging in with Google", task.getException());
                        }
                    });

        } catch (ApiException e) {
            Log.w("GOOGLE AUTH FAILURE", "signInResult:failed code=" + e.getStatusCode());
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    // TODO
    // 9. Handle the result that Google sends back to us
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 10. If this is from the Google request, call the handler method you created above
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
            return;
        }
    }

}
