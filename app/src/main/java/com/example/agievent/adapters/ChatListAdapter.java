package com.example.agievent.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.agievent.R;
import com.example.agievent.models.Chat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ChatListAdapter extends ArrayAdapter<Chat> {
    private Context context;
    private LayoutInflater inflater;

    public ChatListAdapter(Context context, List<Chat> chats) {
        super(context, R.layout.item_chat_list, chats);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_chat_list, parent, false);
            holder = new ViewHolder();
            holder.txtUserName = convertView.findViewById(R.id.txt_user_name);
            holder.txtLastMessage = convertView.findViewById(R.id.txt_last_message);
            holder.txtTimestamp = convertView.findViewById(R.id.txt_timestamp);
            holder.txtUnreadCount = convertView.findViewById(R.id.txt_unread_count);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Chat chat = getItem(position);
        if (chat != null) {
            holder.txtUserName.setText(chat.getOtherUserName());
            holder.txtLastMessage.setText(chat.getLastMessage());
            holder.txtTimestamp.setText(formatTimestamp(chat.getLastTimestamp()));

            // Show/hide unread count
            if (chat.getUnreadCount() > 0) {
                holder.txtUnreadCount.setVisibility(View.VISIBLE);
                holder.txtUnreadCount.setText(String.valueOf(chat.getUnreadCount()));
            } else {
                holder.txtUnreadCount.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp == 0) return "";

        Date date = new Date(timestamp);
        Date now = new Date();

        long diffInMillis = now.getTime() - timestamp;
        long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        if (diffInDays == 0) {
            // Today - show time
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
        } else if (diffInDays == 1) {
            // Yesterday
            return "Yesterday";
        } else if (diffInDays < 7) {
            // Within a week - show day name
            return new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
        } else {
            // Older - show date
            return new SimpleDateFormat("MMM dd", Locale.getDefault()).format(date);
        }
    }

    static class ViewHolder {
        TextView txtUserName;
        TextView txtLastMessage;
        TextView txtTimestamp;
        TextView txtUnreadCount;
    }
}