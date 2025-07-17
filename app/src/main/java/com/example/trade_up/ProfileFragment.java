package com.example.trade_up;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Launch UserProfileActivity when this fragment is shown
        Intent intent = new Intent(getActivity(), UserProfileActivity.class);
        startActivity(intent);
        // Optionally, return an empty view or a loading indicator
        return new android.widget.FrameLayout(getContext());
    }
}
