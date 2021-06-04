package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.example.chat.Adapter.MessageAdapter;
import com.example.chat.Fragments.APIService;
import com.example.chat.Model.Chat;
import com.example.chat.Model.User;
import com.example.chat.Notification.Client;
import com.example.chat.Notification.Data;
import com.example.chat.Notification.MyResponse;
import com.example.chat.Notification.Sender;
import com.example.chat.Notification.Token;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;
    ImageButton btn_send;
    EditText text_send;

    FirebaseUser fuser;
    DatabaseReference reference;

    MessageAdapter messageAdapter;
    List<Chat> mchat;

    RecyclerView recyclerView;

    Intent intent;

    APIService apiService;

    boolean notify = false;

    ValueEventListener seenListener;// check seen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar =findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

     //   toolbar.setNavigationOnClickListener((v -> finish()));

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(MessageActivity.this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        //Notification

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);
        //

    /// hiển thị tin nhắn
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        //lay noi dung tu duoi cung len
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

    ////
        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);

        intent= getIntent();
        final String useid = intent.getStringExtra("userid");
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                String msg = text_send.getText().toString();
                if(!msg.equals("")){
                    sendMessage(fuser.getUid(),useid,msg);
                }else {
                    Toast.makeText(MessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
                }
                text_send.setText("");
            }
        });


        reference = FirebaseDatabase.getInstance().getReference("Users").child(useid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")){
                    profile_image.setImageResource(R.mipmap.ic_launcher);

                }
                else {

                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }
                readMesagges(fuser.getUid(),useid,user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        seenMessge(useid);
    }

    private void seenMessge(String userid){
        reference =FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()){
                    Chat chat = snapshot1.getValue(Chat.class);
                    if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(userid)){
                        HashMap<String,Object> hashMap = new HashMap<>();
                        hashMap.put("isseen",true);
                        snapshot1.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }
    private  void sendMessage(String sender,String receiver, String message){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        final String useid = intent.getStringExtra("userid");
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("sender",sender);
        hashMap.put("receiver",receiver);
        hashMap.put("message",message);
        hashMap.put("isseen",false);

        //dua tin nhan vào db chats
        reference.child("Chats").push().setValue(hashMap);

        //add user to chat fragment
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chatlist")
                .child(fuser.getUid())
                .child(useid);
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    chatRef.child("id").setValue(useid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //Notification
        final String msg = message;
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (notify) {
                    sendNotification(receiver, user.getUsername(), msg);
                }
                notify =false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private  void sendNotification(String receiver , final String username , final String message){
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        final String useid = intent.getStringExtra("userid");
        Query query = tokens.orderByKey().equalTo(receiver);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()){
                    Token token = snapshot1.getValue(Token.class);
                    Data data = new Data(fuser.getUid(),R.mipmap.ic_launcher,username+":"+message,"New Message",useid);
                    Sender sender = new Sender(data,token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200){
                                        if (response.body().success !=1){
                                            Toast.makeText(MessageActivity.this, "Fauiled", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    ///hiển thị tin nhắn
    private  void readMesagges(String myid,String userid,String imageurl){
        mchat = new ArrayList<>();
        reference  =FirebaseDatabase.getInstance().getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mchat.clear();
                for (DataSnapshot saSnapshot1 : snapshot.getChildren()){
                    Chat chat = saSnapshot1.getValue(Chat.class);
                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(myid)){
                        mchat.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this,mchat,imageurl);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    ///
    //status
    private  void status(String status){
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        HashMap<String , Object> hashMap = new HashMap<>();
        hashMap.put("status",status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {

        super.onPause();
        reference.removeEventListener(seenListener);
        status("offline");
    }
    //
}