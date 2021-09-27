package com.example.ev_android.ui.Register;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ev_android.MainActivity;
import com.example.ev_android.R;
import com.example.ev_android.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    private RegisterViewModel galleryViewModel;
    private FragmentRegisterBinding binding;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    View root;

    private static String CAR_NAME;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        galleryViewModel =
                new ViewModelProvider(this).get(RegisterViewModel.class);

        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        Button Btn = (Button) root.findViewById(R.id.register);
        Btn.setEnabled(true);

        Btn.setOnClickListener(new View.OnClickListener(){
            @Override
            // 버튼 입력 시 실행
            public void onClick(View v) {
                //저장된 값 초기화
                clear();

                setCarName();

            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void setCarName(){
        TextView carname = (TextView) root.findViewById(R.id.carname);
        String CAR_NAME = carname.getText().toString();

        Bundle bundle = new Bundle();
        bundle.putString("ID",CAR_NAME);

        Toast.makeText(root.getContext(), "등록되었습니다.", Toast.LENGTH_SHORT).show();

        // 자동차 이름 메인 엑티비티에 전달
        ((MainActivity)getActivity()).setCAR_NAME(CAR_NAME);

        // 앱을 다시 실행시켜도 정보가 유지될 수 있도록 자동차 이름 사용자 기기에 저장
        pref = getActivity().getSharedPreferences("pref", Context.MODE_PRIVATE);
        editor = pref.edit();
        editor.putString("CAR_NAME", CAR_NAME);
        editor.commit();
    }

    public void clear() {

        SharedPreferences prefs = getActivity().getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();

    }
}

