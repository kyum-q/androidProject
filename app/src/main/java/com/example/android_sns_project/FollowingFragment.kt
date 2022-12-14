package com.example.android_sns_project

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android_sns_project.data.Content
import com.example.android_sns_project.data.UserInfo
import com.example.android_sns_project.data.UserList
import com.example.android_sns_project.databinding.ContentItemBinding
import com.example.android_sns_project.databinding.FragmentFollowingBinding
import com.example.android_sns_project.databinding.FragmentUserBinding
import com.example.android_sns_project.databinding.UserListItemBinding
import com.google.common.base.CharMatcher.invisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


class FollowingFragment : Fragment() {
    // TODO: Rename and change types of parameters
    val db = Firebase.firestore
    val rootRef = Firebase.storage.reference
    private var adapter: FollowingFragment.userListAdapter? = null
    private val itemsCollectionRef = db.collection("content")
    private var binding: FragmentFollowingBinding? = null

    var auth: FirebaseAuth? = null
    var currentEmail: String? = null
    var curruntNickname : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFollowingBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        // recyclerview setup

        currentEmail = arguments?.getString("email")

        binding!!.recyclerView.layoutManager = LinearLayoutManager(activity)
        adapter = userListAdapter()
        binding!!.recyclerView.adapter = adapter
        return binding?.root
    }

    inner class MyViewHolder(val binding: UserListItemBinding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("SuspiciousIndentation")
    inner class userListAdapter : RecyclerView.Adapter<FollowingFragment.MyViewHolder>() {

        private val itemsCollectionRef = db.collection("content")
        var userInfos: ArrayList<UserInfo> = arrayListOf()
        var currentUserInfo : UserInfo? = null;
        var userInfo : UserInfo? = null;
        var followingList :  ArrayList<UserInfo> = arrayListOf()
        var follwingEmailList : ArrayList<String> = arrayListOf()
        var emailList : ArrayList<String> = arrayListOf()
        init {
            db.collection("UserInfo").addSnapshotListener{snapshot, error ->

                if (snapshot == null) return@addSnapshotListener
                //?????? ?????? ?????? ????????????
                for(snapshot in snapshot.documents) {
                    var userInfo = snapshot.toObject(UserInfo::class.java)

                    if(!emailList.contains(userInfo?.email!!)){
                        userInfos.add(userInfo)
                        emailList.add(userInfo.email!!)
                    }
//                    if(!userInfos.contans(iuserInfo!!)){
//                        userInfos.add(userInfo)
//                    }

                }
                userInfos.forEach {
                    if(it.email == currentEmail ){
                        //?????? ????????? ????????? ?????????
                        currentUserInfo = it
                    }
                    if(it.email == auth?.currentUser?.email){
                        userInfo  = it
                    }
                }
                //????????? ???????????? userInfo ??????
                currentUserInfo?.followings?.map{(key, value) ->
                    userInfos.forEach{
                        if(it.email == key){
                            if(!follwingEmailList.contains(it.email)){
                                Log.d("followList","followingList${it}" )
                                followingList.add(it)
                                follwingEmailList.add(it.email!!)
                            }

                        }
                    }
                }

                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding: UserListItemBinding = UserListItemBinding.inflate(inflater, parent, false)
            return MyViewHolder(binding)
        }



        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            Log.d("followList",followingList[position].email.toString() )
            val item = followingList[position]
            holder.binding.nickName.text= item.nickname
            holder.binding.nickName.setOnClickListener {
                //????????? ??????????????? ????????????
                var bundle = Bundle()
                bundle.putString("email", item.email)
                findNavController().navigate(R.id.action_followingFragment_to_otherUserFragment,bundle)
            }

            //??????????????? ????????? ?????? hidden??????
            if(item.email == auth?.currentUser?.email){
                holder.binding.accountBtnFollow.setVisibility(View.INVISIBLE)
            }

            //???????????? ????????? ????????? ????????????
            db.collection("UserInfo")?.document(auth?.currentUser?.email!!)
                ?.addSnapshotListener{ snapshot, error ->
                    var userInfo1 = snapshot?.toObject(UserInfo::class.java)
                    curruntNickname = userInfo1?.nickname
                }

            //????????? ???????????? ???????????? button??? following ??????
            db.collection("UserInfo").document(auth?.currentUser?.email!!)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot == null) return@addSnapshotListener

                    userInfo = snapshot.toObject(UserInfo::class.java)
                    if(userInfo?.followings?.containsKey(item.email)!!){
                        holder.binding.accountBtnFollow.text = "????????????"
                    }else{
                        holder.binding.accountBtnFollow.text = "?????????"
                    }
                }
            holder.binding.accountBtnFollow.setOnClickListener {
                db.collection("UserInfo").document(userInfo?.email.toString())
                    .get().addOnSuccessListener {
                        var userInfo2 = it.toObject(UserInfo::class.java)
                        //????????? ?????????
                        //?????? ????????? ????????????
                        if (userInfo2?.followings?.containsKey(item.email)!!) {
                            userInfo2?.followingCount = userInfo2?.followingCount!! - 1
                            userInfo2?.followings!!.remove(item.email)
                            holder.binding.accountBtnFollow.text = "?????????"

                        } else {
                            userInfo2?.followingCount = userInfo2?.followingCount!! + 1
                            Log.d("follow", "follow(email) ${auth?.currentUser?.email}")
                            userInfo2?.followings!![item.email.toString()] = true
                            holder.binding.accountBtnFollow.text = "????????????"

                            if(!item.email.equals(auth?.currentUser?.email))
                                FcmPush.instance.sendMessage(item.email!!, "?????? ???????????? ?????????????????????","",
                                    curruntNickname.toString())
                        }
                        userInfo = userInfo2
                        db.collection("UserInfo").document(userInfo?.email.toString()).set(userInfo!!)
                    }


                db.collection("UserInfo").document(item?.email.toString())
                    .get().addOnSuccessListener {
                        var item = it.toObject(UserInfo::class.java)
                        //????????? ?????????
                        if (item?.followers?.containsKey(userInfo?.email)!!) {
                            item?.followerCount = item?.followerCount!! - 1
                            item?.followers!!.remove(userInfo?.email)
                        } else {
                            item?.followerCount = item?.followerCount!! + 1
                            Log.d("follow", "follow(email) ${auth?.currentUser?.email}")
                            item?.followers!![userInfo?.email!!] = true
                        }
                        db.collection("UserInfo").document(item?.email.toString()).set(item!!)
                    }

            }

        }

        override fun getItemCount(): Int {
            return followingList.size
        }
    }
}