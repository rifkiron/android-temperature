package com.example.sensortemperature;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TemperatureAdapter extends RecyclerView.Adapter<TemperatureAdapter.ViewHolder> {

    private Context mContext;
    private Cursor mCursor;

    public TemperatureAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.temperature_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!mCursor.moveToPosition(position)) {
            return;
        }

        float temperature = mCursor.getFloat(mCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEMPERATURE));
        long timestamp = mCursor.getLong(mCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));

        holder.textTemperature.setText(String.format(Locale.getDefault(), "%.1f °C", temperature));
        holder.textTimestamp.setText(formatDate(timestamp));
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        if (mCursor != null) {
            mCursor.close();
        }

        mCursor = newCursor;

        if (newCursor != null) {
            notifyDataSetChanged();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTemperature;
        TextView textTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTemperature = itemView.findViewById(R.id.textTemperature);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
