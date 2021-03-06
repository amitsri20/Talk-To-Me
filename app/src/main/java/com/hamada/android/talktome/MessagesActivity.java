package com.hamada.android.talktome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hamada.android.talktome.Adapter.MessagesAdapter;
import com.hamada.android.talktome.Model.LastSeenTime;
import com.hamada.android.talktome.Model.Messages;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesActivity extends AppCompatActivity {
    public static String TAG=MessagesActivity.class.getSimpleName();
    private String mChat_id;
    private DatabaseReference mReference;
    @BindView(R.id.message_toolbar)
    Toolbar mToolbar;
    private TextView tv_nameUser;

    private TextView tv_list_seen;

    private CircleImageView circleImageView;
    @BindView(R.id.chat_add_btn)
    ImageButton mAddImageButton;
    @BindView(R.id.chat_send_btn)
    ImageButton mSendImageButton;
    @BindView(R.id.chat_message_view)
    EditText mEditText;
    private FirebaseAuth mAuth;
    private String mCurrent_Id;

    @BindView(R.id.recycler_messages_list)
    RecyclerView mRecyclerView;
    private MessagesAdapter mMessagesAdapterter;
    private List<Messages>  mMessagesList=new ArrayList<>();
    private LinearLayoutManager manager;
    final int ACTIVITY_SELECT_IMAGE = 1234;
    private StorageReference mImageStorage;
    @BindView(R.id.swipe)
    SwipeRefreshLayout  mRefreshLayout;
    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;
    private int itemPos = 0;

    private String mLastKey = "";
    private String mPrevKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        ActionBar actionBar=getSupportActionBar();
        if (actionBar !=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        mReference=FirebaseDatabase.getInstance().getReference();

        mChat_id =getIntent().getExtras()
                .get(UsersActivity.USER_ID).toString();

         mReference.keepSynced(true);

         mAuth=FirebaseAuth.getInstance();
         mCurrent_Id =mAuth.getCurrentUser().getUid();

        String name=getIntent().getExtras().get("nameuser").toString();
        //set recyclerView

        mMessagesAdapterter=new MessagesAdapter(mMessagesList);
        manager =new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mMessagesAdapterter);
        FetchMessages();

        LayoutInflater inflater= (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View action_bar=inflater.inflate(R.layout.chat_custom_layout,null);
        actionBar.setCustomView(action_bar);

        //to make seen for message
        mReference.child("Chat").child(mCurrent_Id).child(mChat_id)
                .child("seen").setValue(true);

        mImageStorage=FirebaseStorage.getInstance().getReference().child("Messages_pictures");
        //
        tv_nameUser=findViewById(R.id.text_view_title);
        tv_list_seen=findViewById(R.id.tv_cust_list_seen);
        circleImageView=findViewById(R.id.custom_image_bar);
         tv_nameUser.setText(name);
        mReference.child("users").child(mChat_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                String lastSeen=  dataSnapshot.child("online").getValue().toString();
                if (lastSeen.equals("true")){
                    tv_list_seen.setText("online");
                }else {
                    LastSeenTime getTime=new LastSeenTime();

                    Long mLastSeen=Long.parseLong(lastSeen);

                    String mDisplayLastSeen=getTime.getTimeAgo(mLastSeen
                            ,getApplicationContext());

                    tv_list_seen.setText(mDisplayLastSeen);
                }

                final String thumb=dataSnapshot.child("user_thumb_image")
                        .getValue().toString();
                Picasso.get().load(thumb).networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.profile)
                        .into(circleImageView, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(Exception e) {

                                Picasso.get().load(thumb).placeholder(R.drawable.profile)
                                        .into(circleImageView);
                            }
                        });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mReference.child("Chat").child(mCurrent_Id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChild(mChat_id)){
                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/" + mCurrent_Id + "/" + mChat_id, chatAddMap);
                    chatUserMap.put("Chat/" + mChat_id + "/" + mCurrent_Id, chatAddMap);

                    mReference.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError != null) {

                                Log.d(TAG, databaseError.getMessage().toString());
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {


            }
        });

        //make click to send message
        mSendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendMessage();
            }
        });


        //for send image
        mAddImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent i = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);

                startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                mCurrentPage++;

                itemPos = 0;

             FetchMessages();

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==ACTIVITY_SELECT_IMAGE &&resultCode == RESULT_OK&&data !=null) {
            Uri imageUri=data.getData();
            //root to sender
            final String message_sender_ref="Messages/"+ mCurrent_Id +"/"+ mChat_id;
            //root to receiver
            final String message_receiver_ref="Messages/"+ mChat_id +"/"+ mCurrent_Id;

            DatabaseReference user_message_key=mReference.child("messages").child(mCurrent_Id)
                    .child(mChat_id).push();
            final String message_push_key=user_message_key.getKey();

            final StorageReference filePath=mImageStorage.child(message_push_key+".jpg");

            filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if (task.isSuccessful()){
                        //to dwonload url for image and save in database
                       filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                           @Override
                           public void onSuccess(Uri uri) {

                               final String dwonloadURL=uri.toString();


                               Map messageBody=new HashMap();

                               messageBody.put("message",dwonloadURL);
                               messageBody.put("seen",false);
                               messageBody.put("type","image");
                               messageBody.put("time",ServerValue.TIMESTAMP);
                               messageBody.put("from", mCurrent_Id);

                               Map messageDetailsBody=new HashMap();

                               messageDetailsBody.put(message_sender_ref+"/"+message_push_key,messageBody);

                               messageDetailsBody.put(message_receiver_ref+"/"+message_push_key,messageBody);

                               mReference.updateChildren(messageDetailsBody, new DatabaseReference.CompletionListener() {
                                   @Override
                                   public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                       if(databaseError != null) {

                                           Log.d(TAG, databaseError.getMessage().toString());
                                       }
                                   }
                               });
                           }
                       });
                    }
                }
            });

        }
    }


    private void FetchMessages() {
        DatabaseReference messageRef = mReference.child("Messages").child(mCurrent_Id).child(mChat_id);

        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

                messageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        Messages messages=dataSnapshot.getValue(Messages.class);

                        itemPos++;

                        if(itemPos == 1){

                            String messageKey = dataSnapshot.getKey();

                            mLastKey = messageKey;
                            mPrevKey = messageKey;

                        }

                        mMessagesList.add(messages);
                        mMessagesAdapterter.notifyDataSetChanged();
                        mRecyclerView.scrollToPosition(mMessagesList.size()-1);
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }


    private void sendMessage(){
        //get vale from edit text
        final String getMessage=mEditText.getText().toString();
        //check for edit text
        if (TextUtils.isEmpty(getMessage)){

        }else {
            //root to sender
            String message_sender_ref="Messages/"+ mCurrent_Id +"/"+ mChat_id;
            //root to receiver
            String message_receiver_ref="Messages/"+ mChat_id +"/"+ mCurrent_Id;

            DatabaseReference user_message_key=mReference.child("messages").child(mCurrent_Id)
                    .child(mChat_id).push();
            String message_push_key=user_message_key.getKey();

            Map messageBody=new HashMap();

            messageBody.put("message",getMessage);
            messageBody.put("seen",false);
            messageBody.put("type","text");
            messageBody.put("time",ServerValue.TIMESTAMP);
            messageBody.put("from", mCurrent_Id);

            Map messageDetailsBody=new HashMap();

            messageDetailsBody.put(message_sender_ref+"/"+message_push_key,messageBody);

            messageDetailsBody.put(message_receiver_ref+"/"+message_push_key,messageBody);

            mEditText.setText("");

            mReference.child("Chat").child(mCurrent_Id).child(mChat_id)
                    .child("seen").setValue(true);
            mReference.child("Chat").child(mCurrent_Id).child(mChat_id)
                    .child("timestamp").setValue(ServerValue.TIMESTAMP);

            mReference.child("Chat").child(mChat_id).child(mCurrent_Id)
                    .child("seen").setValue(false);

            mReference.child("Chat").child(mChat_id).child(mCurrent_Id)
                    .child("timestamp").setValue(ServerValue.TIMESTAMP);

            mReference.updateChildren(messageDetailsBody, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                    if (databaseError !=null){
                        Log.d(TAG,databaseError.getMessage().toString());
                    }

                }
            });

        }


    }
}
