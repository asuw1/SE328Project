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
import com.example.agievent.models.NewsPost;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsPostAdapter extends ArrayAdapter<NewsPost> {
    private Context context;
    private LayoutInflater inflater;

    // Constructor that accepts posts list
    public NewsPostAdapter(Context context, List<NewsPost> posts) {
        super(context, R.layout.item_news_post, posts);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    // Update posts
    public void setPosts(List<NewsPost> posts) {
        clear();
        addAll(posts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_news_post, parent, false);
            holder = new ViewHolder();
            holder.txtAuthor = convertView.findViewById(R.id.txt_author);
            holder.txtContent = convertView.findViewById(R.id.txt_content);
            holder.txtTimestamp = convertView.findViewById(R.id.txt_timestamp);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        NewsPost post = getItem(position);
        if (post != null) {
            // Set post data
            holder.txtAuthor.setText(post.getAuthorName());
            holder.txtContent.setText(post.getContent());
            holder.txtTimestamp.setText(formatTimestamp(post.getTimestamp()));
        }

        return convertView;
    }

    // Helper method
    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date(timestamp));
    }

    // ViewHolder
    static class ViewHolder {
        TextView txtAuthor;
        TextView txtContent;
        TextView txtTimestamp;
    }
}