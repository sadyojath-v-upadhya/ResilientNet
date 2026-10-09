package com.resilientnet.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.resilientnet.R;
import com.resilientnet.ui.home.model.QuickAction;

import java.util.List;

public class QuickActionAdapter extends RecyclerView.Adapter<QuickActionAdapter.ViewHolder> {

    private final List<QuickAction> actionList;
    private final OnQuickActionClickListener listener;
    public QuickActionAdapter(
            List<QuickAction> actionList,
            OnQuickActionClickListener listener) {

        this.actionList = actionList;
        this.listener = listener;
    }

    public void updateActions(List<QuickAction> newActions) {
        actionList.clear();
        if (newActions != null) {
            actionList.addAll(newActions);
        }
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_action, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        QuickAction action = actionList.get(position);

        holder.icon.setImageResource(action.getIcon());
        holder.title.setText(action.getTitle());
        holder.subtitle.setText(action.getSubtitle());
        holder.itemView.setOnClickListener(v -> {
            listener.onQuickActionClicked(action);
        });
    }

    @Override
    public int getItemCount() {
        return actionList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title;
        TextView subtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.imgAction);
            title = itemView.findViewById(R.id.txtTitle);
            subtitle = itemView.findViewById(R.id.txtSubtitle);
        }
    }
}