package com.s23010301.taskping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OnBoardingAdapter extends RecyclerView.Adapter<OnBoardingAdapter.OnBoardingViewHolder> {

    private final List<OnBoardingItem> onboardingItems;

    public OnBoardingAdapter(List<OnBoardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnBoardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Make sure the layout file name below matches the XML file you've actually saved.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false); // or R.layout.item_onboarding
        return new OnBoardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnBoardingViewHolder holder, int position) {
        holder.bind(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    static class OnBoardingViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageOnboarding;
        private final TextView textTitle;
        private final TextView textDescription;

        OnBoardingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageOnboarding = itemView.findViewById(R.id.imageOnboarding);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
        }

        void bind(OnBoardingItem item) {
            imageOnboarding.setImageResource(item.getImage());
            textTitle.setText(item.getTitle());
            textDescription.setText(item.getDescription());
        }
    }
}
