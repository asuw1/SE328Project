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
import com.example.agievent.models.Reply;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReplyAdapter extends ArrayAdapter<Reply> {
    private Context context;
    private LayoutInflater inflater;

    public ReplyAdapter(Context context, List<Reply> replies) {
        super(context, R.layout.item_reply, replies);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_reply, parent, false);
            holder = new ViewHolder();
            holder.txtAuthor = convertView.findViewById(R.id.txt_author);
            holder.txtContent = convertView.findViewById(R.id.txt_content);
            holder.txtTimestamp = convertView.findViewById(R.id.txt_timestamp);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Reply reply = getItem(position);
        if (reply != null) {
            holder.txtAuthor.setText(reply.getAuthorName());
            holder.txtContent.setText(reply.getContent());
            holder.txtTimestamp.setText(formatTimestamp(reply.getTimestamp()));
        }

        return convertView;
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date(timestamp));
    }

    static class ViewHolder {
        TextView txtAuthor;
        TextView txtContent;
        TextView txtTimestamp;
    }
}