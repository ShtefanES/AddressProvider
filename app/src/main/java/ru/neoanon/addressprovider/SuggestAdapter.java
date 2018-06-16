package ru.neoanon.addressprovider;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SuggestAdapter extends RecyclerView.Adapter<SuggestAdapter.MyViewHolder> {
    public interface OnSuggestClickListener {
        void onSuggestionClick(String suggestionStr);
    }

    private List<String> suggestionList;
    private OnSuggestClickListener clickListener;

    public SuggestAdapter(OnSuggestClickListener clickListener) {
        this.clickListener = clickListener;
        suggestionList = new ArrayList<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion,
                parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final String suggestion = suggestionList.get(position);
        holder.tvSuggestion.setText(suggestion);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onSuggestionClick(suggestion);
                }
            }
        });
        if (position == suggestionList.size() - 1) {
            holder.line.setVisibility(View.GONE);
        } else {
            holder.line.setVisibility(View.VISIBLE);
        }
    }

    public void updateData(List<String> suggestionList) {
        this.suggestionList.clear();
        this.suggestionList.addAll(suggestionList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return suggestionList.size();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_suggestion)
        TextView tvSuggestion;
        @BindView(R.id.v_line)
        View line;

        public MyViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
