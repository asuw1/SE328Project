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
import com.example.agievent.models.AgendaItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AgendaAdapter extends ArrayAdapter<AgendaItem> {
    private Context context;
    private LayoutInflater inflater;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;

    public AgendaAdapter(Context context, List<AgendaItem> items) {
        super(context, R.layout.item_agenda, items);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_agenda, parent, false);
            holder = new ViewHolder();
            holder.txtTime = convertView.findViewById(R.id.txt_time);
            holder.txtTitle = convertView.findViewById(R.id.txt_title);
            holder.txtDescription = convertView.findViewById(R.id.txt_description);
            holder.txtLocation = convertView.findViewById(R.id.txt_location);
            holder.txtDate = convertView.findViewById(R.id.txt_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AgendaItem item = getItem(position);
        if (item != null) {
            // Format time range
            String startTime = timeFormat.format(new Date(item.getStartTime()));
            String endTime = timeFormat.format(new Date(item.getEndTime()));
            String timeRange = startTime + " - " + endTime;

            holder.txtTime.setText(timeRange);
            holder.txtTitle.setText(item.getTitle());
            holder.txtDescription.setText(item.getDescription());
            holder.txtLocation.setText(item.getLocation());
            holder.txtDate.setText(dateFormat.format(new Date(item.getStartTime())));
        }

        return convertView;
    }

    static class ViewHolder {
        TextView txtTime;
        TextView txtTitle;
        TextView txtDescription;
        TextView txtLocation;
        TextView txtDate;
    }
}