package com.atakmap.android.apolloedge.login;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.apolloedge.apollo_utils.NetworkTask;
import com.atakmap.android.apolloedge.plugin.R;

public class LoginActivity extends AppCompatActivity implements NetworkTask.AsyncResponse {
    private Login login;

    private EditText editUsername, editPassword;
    private Button submitLoginButton;
    private TextView loginErrorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        editUsername = (EditText) findViewById(R.id.username_input);
        editPassword = (EditText) findViewById(R.id.password_input);
        submitLoginButton = (Button) findViewById(R.id.login_btn);
        loginErrorTextView = (TextView) findViewById(R.id.login_error_text);

        submitLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginActivity.this.submitLogin();
            }
        });

        editUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                LoginActivity.this.onTextChanged(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        editPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                LoginActivity.this.onTextChanged(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    public boolean userAndPasswordEntered() {
        if(this.editUsername.getText().length() > 0
            && this.editPassword.getText().length() > 0)
        {
            //Toast.makeText(this, "true", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        this.loginErrorTextView.setVisibility(View.INVISIBLE);
        if (userAndPasswordEntered()) {
            this.submitLoginButton.setEnabled(true);
        }
        else {
            this.submitLoginButton.setEnabled(false);
        }
    }

    public void submitLogin() {
        Login login = new Login(this.editUsername.getText().toString(), this.editPassword.getText().toString());
        LoginNetworkTask loginNetworkTask = new LoginNetworkTask(this, login);
        loginNetworkTask.delegate = (LoginNetworkTask.AsyncResponse) this; // Set up to get result back
        loginNetworkTask.execute();
    }

    // Override LoginNetworkTask's AsyncResponse method
    @Override
    public void processFinish(String authResult) {
        // Receive the result of onPostExecute
        if (authResult.equals("error")) {
            // Inform the user if their login failed
            this.editPassword.setText("");
            this.loginErrorTextView.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "login successful", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
