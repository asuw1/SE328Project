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
import com.example.agievent.models.Announcement;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends ArrayAdapter<Announcement> {
    private Context context;
    private LayoutInflater inflater;
    private SimpleDateFormat dateFormat;

    public AnnouncementAdapter(Context context, List<Announcement> announcements) {
        super(context, R.layout.item_announcement, announcements);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_announcement, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = convertView.findViewById(R.id.txt_title);
            holder.txtContent = convertView.findViewById(R.id.txt_content);
            holder.txtAuthor = convertView.findViewById(R.id.txt_author);
            holder.txtTimestamp = convertView.findViewById(R.id.txt_timestamp);
            holder.priorityIndicator = convertView.findViewById(R.id.priority_indicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Announcement announcement = getItem(position);
        if (announcement != null) {
            holder.txtTitle.setText(announcement.getTitle());
            holder.txtContent.setText(announcement.getContent());
            holder.txtAuthor.setText("By " + announcement.getAuthorName());
            holder.txtTimestamp.setText(dateFormat.format(new Date(announcement.getTimestamp())));

            // Set priority indicator color
            switch (announcement.getPriority()) {
                case 2: // Urgent
                    holder.priorityIndicator.setBackgroundColor(context.getColor(R.color.error_red));
                    break;
                case 1: // Important
                    holder.priorityIndicator.setBackgroundColor(context.getColor(R.color.warning_orange));
                    break;
                default: // Normal
                    holder.priorityIndicator.setBackgroundColor(context.getColor(R.color.primary_purple_light));
                    break;
            }
        }

        return convertView;
    }

    static class ViewHolder {
        TextView txtTitle;
        TextView txtContent;
        TextView txtAuthor;
        TextView txtTimestamp;
        View priorityIndicator;
    }
}