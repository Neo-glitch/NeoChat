package com.neo.neochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class RegistrationActivity extends AppCompatActivity {
    private CountryCodePicker ccp;
    private EditText phoneText, codeText;
    private Button continueAndNextBtn;
    private String checker = "", phoneNumber = "";
    private RelativeLayout mRelativeLayout;    // relativeLayout holding phone related views

    // firebase
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;  // listens for phoneNumber auth state
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken mResendToken;    // enables resending of verification token or code

    private String mVerificationId;
    private ProgressDialog loadingBar;
    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        phoneText = findViewById(R.id.phoneText);
        codeText = findViewById(R.id.codeText);
        continueAndNextBtn = findViewById(R.id.continueNextButton);
        mRelativeLayout = findViewById(R.id.phoneAuth);
        ccp = findViewById(R.id.ccp);
        ccp.registerCarrierNumberEditText(phoneText);  // reg the CountryCodePicker with the phone num editText

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        continueAndNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(continueAndNextBtn.getText().equals("submit") || checker.equals("code sent")){  // the code has been sent
                    String verificationCode = codeText.toString();  // gets 6 digit code sent to device via sms
                    if(verificationCode.equals("")){
                        codeText.setError("Please enter the sent verification code");
                    } else{
                        loadingBar.setTitle("token verification");
                        loadingBar.setMessage("Please wait, while verification of token takes place");
                        loadingBar.setCanceledOnTouchOutside(false);
                        loadingBar.show();

                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                        signInWithPhoneAuthCredential(credential);
                    }

                } else{  // verification code has not been sent
                    phoneNumber = ccp.getFullNumberWithPlus();
                    if(phoneNumber!= null){
                        loadingBar.setTitle("phone number verification");
                        loadingBar.setMessage("Please wait, while verification is taking place");
                        loadingBar.setCanceledOnTouchOutside(false);
                        loadingBar.show();

                     // sends the verification code to phoneNumber
                        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                phoneNumber,
                                60,    // timeout
                                TimeUnit.SECONDS,
                                RegistrationActivity.this,
                                mCallbacks);
                    } else{
                        Toast.makeText(RegistrationActivity.this, "Please input your phone number", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                // when token is auto-retrieved or phone num instantly verified
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(RegistrationActivity.this, "The phone number isn't valid", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
                mRelativeLayout.setVisibility(View.VISIBLE);
                continueAndNextBtn.setText("Continue");
                codeText.setVisibility(View.GONE);
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);  // just called when token is sent
                mVerificationId = s;
                mResendToken = forceResendingToken;

                mRelativeLayout.setVisibility(View.GONE);
                checker = "code sent";
                continueAndNextBtn.setText("submit");
                codeText.setVisibility(View.VISIBLE);

                loadingBar.dismiss();
                Toast.makeText(RegistrationActivity.this, "code has been sent to phone", Toast.LENGTH_SHORT).show();
            }
        };
    }


    /**
     * Sings in user using the PhoneAuth Credential
     */
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            loadingBar.dismiss();
                            sendUserToMainActivity();
                        } else {
                            // Sign in failed, display a message and update the UI
                            loadingBar.dismiss();
                            String e = task.getException().toString();
                            Toast.makeText(RegistrationActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }

    private void sendUserToMainActivity(){
        startActivity(new Intent(this, ContactsActivity.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // to skip verification stuff if user has already logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            startActivity(new Intent(this, ContactsActivity.class));
            finish();
        }
    }
}