package com.example.projectdraft1.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.projectdraft1.R;
import com.example.projectdraft1.models.User;

import java.util.List;

public class UserSelectionAdapter extends ArrayAdapter<User> {
    private Context context;
    private LayoutInflater inflater;

    public UserSelectionAdapter(Context context, List<User> users) {
        super(context, R.layout.item_user_selection, users);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_user_selection, parent, false);
            holder = new ViewHolder();
            holder.txtUserName = convertView.findViewById(R.id.txt_user_name);
            holder.txtUserEmail = convertView.findViewById(R.id.txt_user_email);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        User user = getItem(position);
        if (user != null) {
            holder.txtUserName.setText(user.getName());
            holder.txtUserEmail.setText(user.getEmail());
        }

        return convertView;
    }

    static class ViewHolder {
        TextView txtUserName;
        TextView txtUserEmail;
    }
}