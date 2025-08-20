package com.s23010301.taskping.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.s23010301.taskping.R;
import com.s23010301.taskping.models.Notification;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notifications;

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title, message, time;
        private final View unreadIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notificationTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            time = itemView.findViewById(R.id.notificationTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }

        void bind(Notification notification) {
            title.setText(notification.getTitle());
            message.setText(notification.getMessage());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            time.setText(sdf.format(notification.getTimestamp()));

            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            itemView.setOnClickListener(v -> {
                // Mark as read and handle click
            });
        }
    }
}