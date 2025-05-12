package com.example.projectdraft1.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectdraft1.R;
import com.example.projectdraft1.activities.PostDetailActivity;
import com.example.projectdraft1.models.NewsPost;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsPostAdapter extends RecyclerView.Adapter<NewsPostAdapter.PostViewHolder> {
    private List<NewsPost> posts;
    private Context context;

    // Simple constructor
    public NewsPostAdapter(Context context) {
        this.context = context;
        this.posts = new ArrayList<>();
    }

    // Update posts
    public void setPosts(List<NewsPost> posts) {
        this.posts = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_news_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        NewsPost post = posts.get(position);

        // Set post data
        holder.txtAuthor.setText(post.getAuthorName());
        holder.txtContent.setText(post.getContent());
        holder.txtTimestamp.setText(formatTimestamp(post.getTimestamp()));

        // Handle click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("post_id", post.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    // ViewHolder
    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView txtAuthor, txtContent, txtTimestamp;

        PostViewHolder(View itemView) {
            super(itemView);
            txtAuthor = itemView.findViewById(R.id.txt_author);
            txtContent = itemView.findViewById(R.id.txt_content);
            txtTimestamp = itemView.findViewById(R.id.txt_timestamp);
        }
    }

    // Helper method
    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date(timestamp));
    }
}