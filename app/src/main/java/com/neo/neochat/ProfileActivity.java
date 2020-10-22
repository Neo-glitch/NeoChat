package com.neo.neochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import static com.neo.neochat.FindPeopleActivity.*;
import static com.neo.neochat.FindPeopleActivity.VISIT_USER_ID;

public class ProfileActivity extends AppCompatActivity {
    public static final String NEW = "new";
    public static final String REQUEST_SENT = "request_sent";
    public static final String REQUEST_RECEIVED = "request_received";
    public static final String REQUEST_ACCEPTED = "request_accepted";

    private String receiverUserID = "",   // uid of user taped on
            receiverUserImage = "", receiverUserName = "";
    private String senderUserId;      // my own id, that'll be used to send friendReq
    private String currentState = "new";


    //widgets
    private ImageView background_profile_view;
    private TextView name_profile;
    private Button add_friend, decline_friend_request;

    //firebase
    private FirebaseAuth mAuth;
    private DatabaseReference friendRequestRef, contactsRef;   // will point to friendReq node and contacts(friends) node


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        senderUserId = mAuth.getCurrentUser().getUid();
        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("Friend Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        Bundle bundle = getIntent().getExtras();

        receiverUserID = bundle.get(VISIT_USER_ID).toString();
        receiverUserImage = bundle.get(PROFILE_IMAGE).toString();
        receiverUserName = bundle.get(PROFILE_NAME).toString();

        background_profile_view = findViewById(R.id.background_profile_view);
        name_profile = findViewById(R.id.name_profile);
        add_friend = findViewById(R.id.add_friend);
        decline_friend_request = findViewById(R.id.decline_friend_request);

        Picasso.get().load(receiverUserImage).into(background_profile_view);
        name_profile.setText((receiverUserName));

        manageClickEvents();
    }

    // handles sending and canceling friend request
    private void manageClickEvents() {

        // retrieves the result from db, incase user leaves activity and come back here
        friendRequestRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.hasChild(receiverUserID)){  // request has been sent to end user and received, but not accepted
                    String requestType = snapshot.child(receiverUserID).child("request_type").getValue().toString();
                    if(requestType.equals("sent")){
                        currentState = REQUEST_SENT;
                        add_friend.setText("Cancel Friend Request");
                    } else if(requestType.equals("received")){
                        currentState = REQUEST_RECEIVED;
                        add_friend.setText("Accept Friend Request");

                        decline_friend_request.setVisibility(View.VISIBLE);
                        decline_friend_request.setOnClickListener(v-> {
                            cancelFriendRequest();
                        });
                    }
                } else{   // request has not been sent or has been accepted
                    contactsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.hasChild(receiverUserID)){  // req already accepted
                                currentState = "friends";
                                add_friend.setText("Delete Contact");
                            } else{  // no friend req sent
                                currentState = NEW;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        if (senderUserId.equals(receiverUserID)) {  // we tapped on our own profile
            add_friend.setVisibility(View.GONE);
        } else {
            add_friend.setOnClickListener(v -> {
                if (currentState.equals(NEW)) {   // we haven't sent request to this person before
                    sendFriendRequest();
                }
                if (currentState.equals(REQUEST_SENT)) {  // friend request has already been sent b4
                    cancelFriendRequest();
                }
                if (currentState.equals(REQUEST_RECEIVED)) {  // friend request received by user and displaying on his end
                    acceptFriendRequest();
                }
                if (currentState.equals(REQUEST_SENT)) {  // friend request has been accepted by end user
                    cancelFriendRequest();
                }
            });
        }
    }


    private void sendFriendRequest() {
        friendRequestRef.child(senderUserId).child(receiverUserID)
                .child("request_type").setValue("sent")   // req_type means req has been sent
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendRequestRef.child(receiverUserID).child(senderUserId)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                currentState = REQUEST_SENT;
                                                add_friend.setText("Cancel Friend Request");
                                                Toast.makeText(getApplicationContext(), "Request sent", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void cancelFriendRequest() {
        friendRequestRef.child(senderUserId).child(receiverUserID).removeValue()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        friendRequestRef.child(receiverUserID).child(senderUserId).removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if(task1.isSuccessful()){
                                        add_friend.setText("Add Friend");
                                        currentState= NEW;
                                    }
                                });
                    }
                });
    }

    private void acceptFriendRequest() {
        contactsRef.child(senderUserId).child(receiverUserID).child("Contact").setValue("Saved")
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        contactsRef.child(receiverUserID).child(senderUserId).child("Contact").setValue("Saved")
                                .addOnCompleteListener(task1 -> {
                                    if(task1.isSuccessful()){
                                        friendRequestRef.child(senderUserId).child(receiverUserID).removeValue()
                                                .addOnCompleteListener(task2 -> {
                                                    if(task2.isSuccessful()){
                                                        friendRequestRef.child(receiverUserID).child(senderUserId).removeValue()
                                                                .addOnCompleteListener(task3 -> {
                                                                    if(task3.isSuccessful()){
                                                                        add_friend.setText("Delete Contact");
                                                                        currentState= "friends";
                                                                        decline_friend_request.setVisibility(View.GONE);
                                                                    }
                                                                });
                                                    }
                                                });

                                    }
                                });
                    }
                });
    }

}