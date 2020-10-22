package com.neo.neochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView notificationsList;
    private String currentUserId;

    //firebase
    private DatabaseReference friendRequestRef, contactsRef, usersRef;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("Friend Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        notificationsList = findViewById(R.id.notifications_list);
        notificationsList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(friendRequestRef.child(currentUserId),   // node to query for request received and sent
                        Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts, NotificationsViewHolder> firebaseRecyclerAdapter
                = new FirebaseRecyclerAdapter<Contacts, NotificationsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull NotificationsViewHolder holder, int position, @NonNull Contacts model) {
                holder.acceptBtn.setVisibility(View.VISIBLE);
                holder.cancelBtn.setVisibility(View.VISIBLE);

                // uid of user in rv list
                final String listUserId = getRef(position).getKey();

                DatabaseReference requestTypeRef = getRef(position).child("request_type").getRef();  // get ref the reqType, value will be sent or Received
                Query query = requestTypeRef.orderByKey();
                query.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String type = snapshot.getValue().toString();
                            if (type.equals("received")) { // received friends requests
                                holder.cardView.setVisibility(View.VISIBLE);

                                usersRef.child(listUserId).addValueEventListener(new ValueEventListener() {  // queries user node of user in rv
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.hasChild("image")) {
                                            final String imageUrl = snapshot.child("image").getValue().toString();
                                            Picasso.get().load(imageUrl).placeholder(R.drawable.profile_image).into(holder.profileImageView);
                                        }
                                        final String name = snapshot.child("name").getValue().toString();
                                        holder.userNameText.setText(name);

                                        holder.acceptBtn.setOnClickListener(v -> {
                                            acceptFriendRequest(listUserId);
                                        });

                                        holder.cancelBtn.setOnClickListener(v -> {
                                            cancelFriendRequest(listUserId);
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            } else {
                                holder.cardView.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @NonNull
            @Override
            public NotificationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.find_friends_item, parent, false);
                return new NotificationsViewHolder(view);
            }
        };

        notificationsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    public static class NotificationsViewHolder extends RecyclerView.ViewHolder {
        TextView userNameText;
        Button acceptBtn, cancelBtn;
        ImageView profileImageView;
        RelativeLayout cardView;

        public NotificationsViewHolder(@NonNull View itemView) {
            super(itemView);

            userNameText = itemView.findViewById(R.id.name_notification);
            acceptBtn = itemView.findViewById(R.id.request_accept_btn);
            cancelBtn = itemView.findViewById(R.id.request_decline_btn);
            profileImageView = itemView.findViewById(R.id.image_notification);
            cardView = itemView.findViewById(R.id.card_view);
        }
    }

    private void acceptFriendRequest(String listUserId) {
        contactsRef.child(currentUserId).child(listUserId).child("Contact").setValue("Saved")
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        contactsRef.child(listUserId).child(currentUserId).child("Contact").setValue("Saved")
                                .addOnCompleteListener(task1 -> {
                                    if(task1.isSuccessful()){
                                        friendRequestRef.child(currentUserId).child(listUserId).removeValue()
                                                .addOnCompleteListener(task2 -> {
                                                    if(task2.isSuccessful()){
                                                        friendRequestRef.child(listUserId).child(currentUserId).removeValue()
                                                                .addOnCompleteListener(task3 -> {
                                                                });
                                                    }
                                                });

                                    }
                                });
                    }
                });
    }


    private void cancelFriendRequest(String listUserId) {
        friendRequestRef.child(currentUserId).child(listUserId).removeValue()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        friendRequestRef.child(listUserId).child(currentUserId).removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if(task1.isSuccessful()){
                                    }
                                });
                    }
                });
    }
}