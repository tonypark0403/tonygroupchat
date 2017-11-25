package com.example.tonypark.tonygroupchat.fragments;


import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.tonypark.tonygroupchat.R;
import com.example.tonypark.tonygroupchat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendFragment extends Fragment {

    @BindView(R.id.friendfragment_linearlayout_search_area)
    LinearLayout mSearchArea; //private 쓰면 butterknife가 접근 못함

    @BindView(R.id.friendfragment_edittext_content)
    EditText edtEmail;

    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mFriendsDBRef;
    private DatabaseReference mUserDBRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View friendView = inflater.inflate(R.layout.fragment_friend, container, false);
        ButterKnife.bind(this, friendView); //fragment는 view도 인자로 넣어줘야 함

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFriendsDBRef = mFirebaseDatabase.getReference("users").child(mFirebaseUser.getUid()).child("friends");
                                                    //"users/" + mFirebaseUser.getUid() + "/friends"
        mUserDBRef = mFirebaseDatabase.getReference("users");
       return friendView;
    }

    public void toggleSearchBar() {
        mSearchArea.setVisibility(mSearchArea.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    //setOnclick event를 butterknife를 이용 : private을 쓸수 없음
    @OnClick(R.id.friendfragment_button_find)
    void addFriend() {
        // 1. 입력된 이메일을 가져옴 -> 이메일 없으면 이메일 입력하라는 메세지
        // 2. 자기자신을 친구로 등록할 수 없기때문에 firebaseUser의 email이 입력한 이메일과 같다면, 자기자신은 등록 불가 메세지 줌
        // 3. 이메일이 정상이라면 나의 정보를 조회하여 이미등록된 친구인지 판단
        // 4. users db에 존재하지 않는 이메일이면 가입하지 않은 친구라는 메세지를 줌
        // 5. users/{myUid}/friends/{someone_uid}/상대 정보를 등록
        // 6. users/{someone_uid}/friends/{myUid}/상대 정보 등록

        // 1. 입력된 이메일을 가져옴 -> 이메일 없으면 이메일 입력하라는 메세지
        final String inputEmail = edtEmail.getText().toString();
        if(inputEmail.isEmpty()) {
            Snackbar.make(mSearchArea, "Input Email", Snackbar.LENGTH_LONG).show();
            return;
        }
        // 2. 자기자신을 친구로 등록할 수 없기때문에 firebaseUser의 email이 입력한 이메일과 같다면, 자기자신은 등록 불가 메세지 줌
        if(inputEmail.equals(mFirebaseUser.getEmail())) {
            Snackbar.make(mSearchArea, "Not possible to add yourself as a friend", Snackbar.LENGTH_LONG).show();
            return;
        }
        // 3. 이메일이 정상이라면 나의 정보를 조회하여 이미등록된 친구인지 판단
        mFriendsDBRef.addListenerForSingleValueEvent(new ValueEventListener() {//SingleValueEvent 한번만 체크하고 또 리슨안함
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> friendsIterable = dataSnapshot.getChildren();
                Iterator<DataSnapshot> friendsIterator = friendsIterable.iterator();

                while(friendsIterator.hasNext()) {
                    User user = friendsIterator.next().getValue(User.class);

                    if(user.getEmail().equals(inputEmail)) { //inputEmail로 비교해야지 mFirebaseUser.getEmail() 나의 이메일로 비교하면 안됨
                        Snackbar.make(mSearchArea, "Already your friend!!!", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                }
                // 4. users db에 존재하지 않는 이메일이면 가입하지 않은 친구라는 메세지를 줌
                mUserDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Iterator<DataSnapshot> userIterator = dataSnapshot.getChildren().iterator();
                        int userCount = (int) dataSnapshot.getChildrenCount();
                        int loopCount = 1;

                        while(userIterator.hasNext()) {
                            final User currentUser = userIterator.next().getValue(User.class);

                            if (inputEmail.equals(currentUser.getEmail())) { // 입력된 이메일이 이 앱에 등록된 유저인지
                                // 친구 등록
                                // 5. users/{myUid}/friends/{someone_uid}/상대 정보를 등록 : 내 목록에 친구등록
                                mFriendsDBRef.push().setValue(currentUser, new DatabaseReference.CompletionListener() { //등록이 잘 되었는지 체크하는 리스너
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        // 6. users/{someone_uid}/friends/{myUid}/상대 정보 등록
                                        // 내정보가 필요
                                        mUserDBRef.child(mFirebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                User myInfo = dataSnapshot.getValue(User.class);
                                                mUserDBRef.child(currentUser.getUid()).child("friends").push().setValue(myInfo);
                                                Snackbar.make(mSearchArea, "Complete friend registration", Snackbar.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                });
                            } else {
                                // 총 사용자의 명수 == loopCount수(while) => 등록된 사용자가 없다고 출력
                                if (loopCount++ >= userCount) {
                                    Snackbar.make(mSearchArea, "Your friend is not on the list!", Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
