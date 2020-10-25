package com.essam.chatapp.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.essam.chatapp.models.User;
import com.essam.chatapp.ui.login.LoginActivity;
import com.essam.chatapp.ui.login.LoginCallbacks;
import com.essam.chatapp.utils.ProjectUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class LoginHelper {
    private LoginCallbacks mCallbacks;
    private FirebaseManager mManager;

    private static final String TAG = "LoginHelper";

    public LoginHelper(LoginCallbacks callbacks) {
        mCallbacks = callbacks;
        mManager = FirebaseManager.getInstance();
    }

    public void getVerificationCode(String phoneNumber, final LoginActivity activity) {
        if (!ProjectUtils.isPhoneNumberValid(phoneNumber)){
            mCallbacks.onInvalidPhoneNumber();
            return;
        }

        // Static for Egypt ISO code now
        phoneNumber = "+20" + phoneNumber;

        // this call back basically overrides three methods
        // 1- onVerificationCompleted : this means verification automatically done and no need to enter verify code
        // 2- onCodeSent : returns a string verification code to users phone number so we have to compare this code with the code that entered by user
        //check automatically for credentials
        PhoneAuthProvider.OnVerificationStateChangedCallbacks loginPhoneCallBack =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                //check automatically for credentials
                signInWithPhoneCredential(phoneAuthCredential, activity);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.e(TAG, "onVerificationFailed: " + e.toString());
                mCallbacks.onInvalidPhoneNumber();
            }

            @Override
            public void onCodeSent(@NonNull String code, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(code, forceResendingToken);
                mCallbacks.onVerificationCodeSent(code);
            }
        };

        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber,
                60,
                TimeUnit.SECONDS,
                activity,
                loginPhoneCallBack);
    }

    public void signInWithPhoneCredential(PhoneAuthCredential phoneAuthCredential, final LoginActivity activity) {
        mManager.getFirebaseAuth().signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    // Tell firebase manager that user has been successfully logged in
                    mManager.updateUserAuthState(UserAuthState.LOGGED_IN);

                    // check if this is a new user or already registered user
                    checkIfUserExistInDataBase();

                    // update view
                    mCallbacks.onLoginSuccess(mManager.getMyPhone());

                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mCallbacks.onInvalidVerificationCode();
            }
        });
    }

    private void checkIfUserExistInDataBase() {
        ValueEventListener userEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    createNewUser();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        mManager.getUserDb().addListenerForSingleValueEvent(userEventListener);
    }

    private void createNewUser() {
        User user = new User(mManager.getMyUid(),
                mManager.getMyPhone(),
                mManager.getMyPhone(),
                "Hey there , I'm using Kroubi",
                "",
                "online");
        mManager.getUserDb().setValue(user);
    }
}