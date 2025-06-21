package com.s23010301.taskping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class OnBoardingAdapter extends RecyclerView.Adapter<OnBoardingAdapter.OnBoardingViewHolder> {

    private final int[] layouts = {
            R.layout.onboarding_screen_1,
            R.layout.onboarding_screen_2,
            R.layout.onboarding_screen_3
    };

    private final AppCompatActivity activity;

    public OnBoardingAdapter(AppCompatActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public OnBoardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(layouts[viewType], parent, false);
        return new OnBoardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnBoardingViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return layouts.length;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class OnBoardingViewHolder extends RecyclerView.ViewHolder {
        public OnBoardingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
