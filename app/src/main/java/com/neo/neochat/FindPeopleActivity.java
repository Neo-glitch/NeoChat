package com.neo.neochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class FindPeopleActivity extends AppCompatActivity {
    public static final String VISIT_USER_ID = "visit_user_id";
    public static final String PROFILE_IMAGE = "profile_image";
    public static final String PROFILE_NAME = "profile_name";
    private RecyclerView findFriendsList;
    private EditText searchEt;
    private String str = "";   // string to hold what user entered to search

    // firebase
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_people);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        searchEt = findViewById(R.id.search_user_text);
        findFriendsList = findViewById(R.id.find_friends_list);
        findFriendsList.setLayoutManager(new LinearLayoutManager(this));

        searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchEt.getText().toString().equals("")) {  // nothing has been entered in Et for search
                    return;
                } else {
                    str = s.toString();
                    onStart();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options = null;
        if (str.equals("")) {
            options = new FirebaseRecyclerOptions.Builder<Contacts>()
                    .setQuery(usersRef, Contacts.class)   // querys ref and retrieves list of modelClass
                    .build();
        } else {  // user search for something
            options = new FirebaseRecyclerOptions.Builder<Contacts>()
                    .setQuery(usersRef.orderByChild("name").startAt(str).endAt(str + "\uf8ff"),
                            Contacts.class)   // queries ref and retrieves list of modelClass and order by name field
                    .build();
        }

        // creates adapter with Contacts list
        FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder> firebaseRecyclerAdapter
                = new FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FindFriendsViewHolder holder, int position, @NonNull Contacts model) {
                holder.userNameText.setText(model.getName());
                Picasso.get().load(model.getImage()).into(holder.profileImageView);

                holder.itemView.setOnClickListener(v -> {
                    String visit_user_id = getRef(position).getKey();     // get query res at the position passed and get the userId

                    Intent intent = new Intent(FindPeopleActivity.this, ProfileActivity.class);
                    intent.putExtra(VISIT_USER_ID, visit_user_id);
                    intent.putExtra(PROFILE_IMAGE, model.getImage());
                    intent.putExtra(PROFILE_NAME, model.getName());
                    startActivity(intent);
                });
            }

            @NonNull
            @Override
            public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_item, parent, false);
                return new FindFriendsViewHolder(view);
            }
        };

        // sets up th rv with the firebase adapter
        findFriendsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    private static class FindFriendsViewHolder extends RecyclerView.ViewHolder {
        TextView userNameText;
        Button videoCallBtn;
        ImageView profileImageView;
        RelativeLayout cardView;

        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            userNameText = itemView.findViewById(R.id.name_contact);
            videoCallBtn = itemView.findViewById(R.id.call_btn);
            profileImageView = itemView.findViewById(R.id.image_contact);
            cardView = itemView.findViewById(R.id.card_view_contact);

            videoCallBtn.setVisibility(View.GONE);    // hides VC btn unless user is friends with user in list
        }
    }
}