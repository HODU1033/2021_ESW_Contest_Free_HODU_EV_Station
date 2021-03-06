package com.example.ev_android.ui.home;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ev_android.FB_info;
import com.example.ev_android.MainActivity;
import com.example.ev_android.MyAdapter;
import com.example.ev_android.R;
import com.example.ev_android.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class HomeFragment extends Fragment {

    static int MAX_STATION = 3;

    static String BATTERY;
    static String IN_TIME;
    static String CAR_NAME;

    static boolean tfStation;
    static boolean Station_arr[] = new boolean[MAX_STATION];
    static int StationNum;

    static String Stationinfo;

    static boolean OC;

    static ImageView Car_color;

    FB_info fi = new FB_info();

    private HomeViewModel homeViewModel;

    private FragmentHomeBinding binding;

    static NotificationManager manager;
    static NotificationCompat.Builder builder;
    static Notification notification;

    private static String CHANNEL_ID = "channel1";
    private static String CHANEL_NAME = "Channel1";

    @RequiresApi(api = Build.VERSION_CODES.O)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        View root = binding.getRoot();

        recyclerView = root.findViewById(R.id.carList);
        database = FirebaseDatabase.getInstance().getReference();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        list = new ArrayList<>();
        myAdapter = new MyAdapter(getActivity(), list);
        recyclerView.setAdapter(myAdapter);

        CAR_NAME = ((MainActivity)getActivity()).getCAR_NAME();

        //????????? ????????? ??????????????? ????????????????????? ???????????? ????????? ???????????? ???????????? ??????
        if (CAR_NAME != null){

            cp_Carname("Station1",0, root.findViewById(R.id.Car_img_01));
            cp_Carname("Station2",1, root.findViewById(R.id.Car_img_02));
            cp_Carname("Station3",2, root.findViewById(R.id.Car_img_03));
        }

        // ?????? ???????????? ????????? ?????? ???????????? ??? ????????????
        Car_img("Station1",0, root.findViewById(R.id.Car_img_01));
        Car_img("Station2",1, root.findViewById(R.id.Car_img_02));
        Car_img("Station3",2, root.findViewById(R.id.Car_img_03));


        // ????????? ?????? ?????? ????????? ????????????
        showNoti();

        if (!tfStation){
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText("????????? ???????????? ???????????? ????????? ???????????? ???????????????.").setBigContentTitle("EV_ANDROID"));
            builder.setOngoing(false);

            notification = builder.build();
            manager.notify(1, notification);
        }
        else{
            editNoti();
        }


        // ????????????????????? ????????? ???????????? ??????
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    // ???????????? ??? ????????? ????????????
                    FB_info info = dataSnapshot.getValue(FB_info.class);

                    if (CAR_NAME != null){

                        cp_Carname("Station1",0, root.findViewById(R.id.Car_img_01));
                        cp_Carname("Station2",1, root.findViewById(R.id.Car_img_02));
                        cp_Carname("Station3",2, root.findViewById(R.id.Car_img_03));
                    }


                    // ????????? ????????? ?????? ???????????? ?????? ????????????
                    if (cnt == MAX_STATION) {
                        Stationinfo = getStation(Station_arr);

                        // ????????????????????? ???????????? ????????? ????????? ?????? ??? ?????????????????? ???????????? ?????? ?????????
                        resetstation();

                        // ???????????? ?????? ????????? ?????? ??????
                        Car_img("Station1",0, root.findViewById(R.id.Car_img_01));
                        Car_img("Station2",1, root.findViewById(R.id.Car_img_02));
                        Car_img("Station3",2, root.findViewById(R.id.Car_img_03));

                        // ???????????? ???????????? ??????????????? ?????? ????????? ?????? ??? ??????????????? ?????????????????? ?????? ???????????? ?????? ??? ????????? ?????? ????????????
                        if(tfStation){
                            database.child(Stationinfo).child("OVER_CHARGING").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    if (!task.isSuccessful()) {
                                        Log.e("firebase", "Error getting data", task.getException());
                                    } else {
                                        if (String.valueOf(task.getResult().getValue()).equals("O") && !OC){
                                            Toast.makeText(root.getContext(), "?????????????????????.", Toast.LENGTH_SHORT).show();


                                            OC = true;
                                        }
                                    }
                                }
                            });

                            database.child(Stationinfo).child("USING").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    if (!task.isSuccessful()) {
                                        Log.e("firebase", "Error getting data", task.getException());
                                    } else {
                                        if (String.valueOf(task.getResult().getValue()).equals("X")) {


                                            database.child(Stationinfo).child("PAYMENT").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                                    if (!task.isSuccessful() && OC) {
                                                        Log.e("firebase", "Error getting data", task.getException());
                                                    } else {
                                                        Toast.makeText(root.getContext(), "?????? ????????? "+ String.valueOf(task.getResult().getValue()) + " ??? ?????????????????????.", Toast.LENGTH_SHORT).show();

                                                        builder.setStyle(new NotificationCompat.BigTextStyle().bigText("?????? ????????? "+ String.valueOf(task.getResult().getValue()) + "??? ?????????????????????.").setBigContentTitle("EV_ANDROID"));
                                                        builder.setOngoing(false);
                                                        notification = builder.build();
                                                        manager.notify(1, notification);

                                                        OC = false;
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                            editNoti();
                        }
                        // ??????????????? ?????? ????????? ?????? ??? ???????????? ??????
                        list.clear();
                        cnt = 0;
                    }

                    cnt++;
                    list.add(info);
                }
                //???????????? ??? ????????? ????????????
                myAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    RecyclerView recyclerView;
    DatabaseReference database;
    MyAdapter myAdapter;
    ArrayList<FB_info> list;

    private int cnt = 0;

    //????????? ????????? ?????? ??????
    public void Car_img(String Station,int num, View view) {
        database.child(Station).child("USING").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    Car_color = (ImageView) view;
                    if (String.valueOf(task.getResult().getValue()).equals("O") && !Station_arr[num]) {
                        Car_color.setImageResource(R.drawable.carimg01);
                    }
                    else if(Station_arr[num]){
                        Car_color.setImageResource(R.drawable.carimg03);
                    }
                    else {
                        Car_color.setImageResource(R.drawable.carimg02);
                    }
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    // ?????? ??? ?????? ??????
    public void showNoti() {

        builder = null;

        manager = (NotificationManager) getActivity().getSystemService(getActivity().NOTIFICATION_SERVICE);
        manager.createNotificationChannel(
                new NotificationChannel(CHANNEL_ID, CHANEL_NAME, NotificationManager.IMPORTANCE_LOW));

        builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID);

        builder.setContentTitle("\n");
        builder.setContentText("");
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText("BATTERY : "+BATTERY+"% IN TIME : "+IN_TIME).setBigContentTitle("EV_ANDROID"));
        builder.setSmallIcon(R.drawable.ic_stat_bolt);
        builder.setOngoing(true);
    }

    // ????????? ?????? ???????????? ???????????? ??????????????? ????????? ??????????????? ??????
    public void cp_Carname(String station, int num, View view){

        database.child(station).child("CAR_NAME").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    if (CAR_NAME.equals(String.valueOf(task.getResult().getValue()))){

                        tfStation = true;
                        Station_arr[num] = true;
                        StationNum = num;
                    }
                }
            }
        });
    }

    // ????????? ??????????????? ????????? ?????? ?????????????????? ??????????????? ??????
    public String getStation(boolean[] setStation){
        if (setStation[0]) {
            return "Station1";
        }
        else if(setStation[1]){
            return "Station2";
        }
        else{
            return "Station3";
        }
    }

    // ????????????????????? ???????????? ????????? ????????? ?????? ??? ?????????????????? ???????????? ?????? ?????????
    public void resetstation() {
        database.child(getStation(Station_arr)).child("USING").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    if (String.valueOf(task.getResult().getValue()).equals("X")) {
                        tfStation = false;

                        for (int i = 0; i < MAX_STATION; i++) {
                            Station_arr[i] = false;
                        }
                    }
                }
            }
        });
    }

    // ?????? ??? ????????? ??????????????? ???????????????????????? ???????????? ??????????????? ??????
    public void editNoti(){
        if (!OC){
            database.child(Stationinfo).child("BATTERY").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        BATTERY = (String.valueOf(task.getResult().getValue()));
                        builder.setStyle(new NotificationCompat.BigTextStyle().bigText("BATTERY : "+BATTERY+"% IN TIME : "+IN_TIME).setBigContentTitle("EV_ANDROID"));

                        notification = builder.build();
                        manager.notify(1, notification);
                    }
                }
            });

            database.child(Stationinfo).child("IN_TIME").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    } else {
                        IN_TIME = (String.valueOf(task.getResult().getValue()));
                        builder.setStyle(new NotificationCompat.BigTextStyle().bigText("BATTERY : "+BATTERY+"% IN TIME : "+IN_TIME).setBigContentTitle("EV_ANDROID"));

                        notification = builder.build();
                        manager.notify(1, notification);
                    }
                }
            });
        }
        else{
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText("?????????????????????. ????????? ?????????????????????.").setBigContentTitle("EV_ANDROID"));

            builder.setOngoing(true);
            notification = builder.build();
            manager.notify(1, notification);
        }
    }
}
