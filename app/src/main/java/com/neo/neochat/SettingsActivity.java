package com.neo.neochat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    public static final int GALLERYREQUEST = 1;
    private static final String TAG = "SettingsActivity";

    //widgets
    private Button saveBtn;
    private EditText userNameEt, userBioEt;
    private ImageView profileImageView;
    private ProgressDialog mProgressDialog;

    private Uri imageUri;   // var to hold image Uri of image gotten from phone gallery
    private String downloadUrl;   // url of image stored on firebase storage after upload

    //firebase
    private StorageReference userProfileImgRef;
    private DatabaseReference userRef;
    private String mGetUsername;
    private String mGetUserStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        userProfileImgRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        saveBtn = findViewById(R.id.save_settings_btn);
        userNameEt = findViewById(R.id.username_settings);
        userBioEt = findViewById(R.id.bio_settings);
        profileImageView = findViewById(R.id.settings_profile_image);
        mProgressDialog = new ProgressDialog(this);
        retrieveUserInfo();

        profileImageView.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GALLERYREQUEST);
        });

        saveBtn.setOnClickListener(v -> {
            saveUserData();   // saves the user data on firebase database
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERYREQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                imageUri = data.getData();
                profileImageView.setImageURI(imageUri);
            }
        }
    }

    private void saveUserData() {
        mGetUsername = userNameEt.getText().toString();
        mGetUserStatus = userBioEt.getText().toString();

        if (mGetUsername.equals("")) {
            userNameEt.setError("No username found");
        }
        if (mGetUserStatus.equals("")) {
            userBioEt.setError("status info not found");
        }

        if (imageUri == null) {  // no image is chosen
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // checks if user has an image url in db previously
                    if (snapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).hasChild("image")) {
                        saveInfoOnly();   // saves only user status and userName
                    } else {
                        Toast.makeText(SettingsActivity.this, "Please set your profile image", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } else {   // image has been chosen
            mProgressDialog.setTitle("Account settings");
            mProgressDialog.setMessage("Updating user profile in progress...");
            mProgressDialog.show();

            final StorageReference filePath = userProfileImgRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            final UploadTask uploadTask = filePath.putFile(imageUri);  // starts an upload to fb, and uploadTask will monitor this
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {  // called after completing uploadTask
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    downloadUrl = filePath.getDownloadUrl().toString();
                    Log.d(TAG, "then: starts: " + downloadUrl);
                    return filePath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        downloadUrl = task.getResult().toString();
                        Log.d(TAG, "onComplete: starts" + downloadUrl);

                        HashMap<String, Object> profileMap = new HashMap<>();
                        profileMap.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        profileMap.put("name", mGetUsername);
                        profileMap.put("status", mGetUserStatus);
                        profileMap.put("image", downloadUrl);

                        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(profileMap)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "onComplete: task Successful");
                                            startActivity(new Intent(SettingsActivity.this, ContactsActivity.class));
                                            finish();
                                            mProgressDialog.dismiss();
                                            Toast.makeText(getApplicationContext(), "update successful", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Update failed, check your internet", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }
            });
        }
    }

    private void saveInfoOnly() {
        mGetUsername = userNameEt.getText().toString();
        mGetUserStatus = userBioEt.getText().toString();

        if (mGetUsername.equals("")) {
            userNameEt.setError("No username found");
        }
        if (mGetUserStatus.equals("")) {
            userBioEt.setError("status info not found");
        } else {
            mProgressDialog.setTitle("Account settings");
            mProgressDialog.setMessage("Updating user profile in progress...");
            mProgressDialog.show();

            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            profileMap.put("name", mGetUsername);
            profileMap.put("status", mGetUserStatus);

            userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                startActivity(new Intent(SettingsActivity.this, ContactsActivity.class));
                                finish();
                                mProgressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "update successful", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Update failed, check your internet", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }


    /**
     * retrieves userInfo from firebase
     */
    private void retrieveUserInfo() {
        Query query = userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String imageDb = snapshot.child("image").getValue().toString();
                    String nameDb = snapshot.child("name").getValue().toString();
                    String bioDb = snapshot.child("status").getValue().toString();

                    userNameEt.setText(nameDb);
                    userBioEt.setText(bioDb);
                    Picasso.get().load(imageDb)
                            .placeholder(getResources().getDrawable(R.drawable.profile_image))
                            .into(profileImageView);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}