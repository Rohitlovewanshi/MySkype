package com.rohit.myskype;

import android.content.Intent;
import android.os.Bundle;
import android.telecom.Call;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView navView;
    RecyclerView myContactsList;
    ImageView findPeopleBtn;

    private DatabaseReference contactsRef,userRef;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private String userName="",profileImage="";
    private String calledBy="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactsRef= FirebaseDatabase.getInstance().getReference().child("Contacts");
        mAuth=FirebaseAuth.getInstance();
        currentUserId=mAuth.getCurrentUser().getUid();
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");

        navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
        findPeopleBtn=findViewById(R.id.find_people_btn);
        myContactsList=findViewById(R.id.contact_list);

        myContactsList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));


        findPeopleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent findPeopleIntent=new Intent(MainActivity.this,FindPeopleActivity.class);
                startActivity(findPeopleIntent);
            }
        });

    }

    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

            switch (menuItem.getItemId()){

                case R.id.navigation_home:
                    Intent mainIntent=new Intent(MainActivity.this,MainActivity.class);
                    startActivity(mainIntent);
                    break;

                case R.id.navigation_settings:
                    Intent settingsIntent=new Intent(MainActivity.this,SettingsActivity.class);
                    startActivity(settingsIntent);
                    break;

                case R.id.navigation_notifications:
                    Intent notificationIntent=new Intent(MainActivity.this,NotificationsActivity.class);
                    startActivity(notificationIntent);
                    break;

                case R.id.navigation_logout:
                    FirebaseAuth.getInstance().signOut();
                    Intent logoutIntent=new Intent(MainActivity.this,RegistrationActivity.class);
                    startActivity(logoutIntent);
                    finish();
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        checkForReceivingCall();

        validateUser();

        FirebaseRecyclerOptions<Contacts> options=new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(contactsRef.child(currentUserId),Contacts.class).build();

        FirebaseRecyclerAdapter<Contacts,ContactsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Contacts, ContactsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ContactsViewHolder holder, int i, @NonNull Contacts contacts) {

                final String listUserId=getRef(i).getKey();

                userRef.child(listUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()){

                            userName=dataSnapshot.child("name").getValue().toString();
                            profileImage=dataSnapshot.child("image").getValue().toString();

                            holder.userNameTxt.setText(userName);
                            Picasso.get().load(profileImage).into(holder.profileImageView);
                        }

                        holder.callBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                Intent callingIntent=new Intent(MainActivity.this, CallingActivity.class);
                                callingIntent.putExtra("visit_user_id",listUserId);
                                startActivity(callingIntent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_design,parent,false);
                ContactsViewHolder viewHolder =new ContactsViewHolder(view);
                return viewHolder;
            }
        };

        myContactsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    private void validateUser() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        reference.child("Users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (!dataSnapshot.exists()){

                    Intent settingIntent=new Intent(MainActivity.this,SettingsActivity.class);
                    startActivity(settingIntent);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public static class ContactsViewHolder extends RecyclerView.ViewHolder {

        TextView userNameTxt;
        Button callBtn;
        ImageView profileImageView;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);

            userNameTxt=itemView.findViewById(R.id.name_contact);
            callBtn=itemView.findViewById(R.id.call_btn);
            profileImageView=itemView.findViewById(R.id.image_contact);
        }
    }

    private void checkForReceivingCall() {

        userRef.child(currentUserId).child("Ringing").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild("ringing")){

                    calledBy=dataSnapshot.child("ringing").getValue().toString();

                    Intent callingIntent=new Intent(MainActivity.this,CallingActivity.class);
                    callingIntent.putExtra("visit_user_id",calledBy);
                    startActivity(callingIntent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
