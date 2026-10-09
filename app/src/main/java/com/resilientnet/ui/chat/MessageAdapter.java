package com.resilientnet.ui.chat;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.resilientnet.R;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.model.MessageConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter
        extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final String TAG = "MessageAdapter";

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<MessageEntity> messages =
            new ArrayList<>();

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("h:mm a", Locale.getDefault());

    // Unique ID of this device
    private String currentDeviceId = null;

    // ---------------------------------------------------------
    // CURRENT DEVICE ID
    // ---------------------------------------------------------

    public void setCurrentDeviceId(String currentDeviceId) {

        this.currentDeviceId = currentDeviceId;

        Log.d(
                TAG,
                "Current device ID = " + currentDeviceId
        );

        notifyDataSetChanged();
    }

    // ---------------------------------------------------------
    // SET MESSAGES
    // ---------------------------------------------------------

    public void setMessages(List<MessageEntity> messages) {

        this.messages.clear();

        if (messages != null) {
            this.messages.addAll(messages);
        }

        notifyDataSetChanged();
    }

    // ---------------------------------------------------------
    // VIEW TYPE
    // ---------------------------------------------------------

    @Override
    public int getItemViewType(int position) {

        MessageEntity message =
                messages.get(position);

        String senderId =
                message.getSenderId();

        /*
         * If the sender ID matches or ends with this device's ID,
         * the message was sent by this device.
         */
        if (senderId != null && currentDeviceId != null) {
            if (senderId.equals(currentDeviceId)
                    || senderId.endsWith(currentDeviceId)
                    || currentDeviceId.endsWith(senderId)) {

                return VIEW_TYPE_SENT;
            }
        }

        return VIEW_TYPE_RECEIVED;
    }

    // ---------------------------------------------------------
    // CREATE VIEW HOLDER
    // ---------------------------------------------------------

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        int layout;

        if (viewType == VIEW_TYPE_SENT) {

            layout =
                    R.layout.item_message_sent;

        } else {

            layout =
                    R.layout.item_message_received;
        }

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                layout,
                                parent,
                                false
                        );

        return new MessageViewHolder(view);
    }

    // ---------------------------------------------------------
    // BIND MESSAGE
    // ---------------------------------------------------------

    @Override
    public void onBindViewHolder(
            @NonNull MessageViewHolder holder,
            int position) {

        MessageEntity message =
                messages.get(position);

        int viewType = getItemViewType(position);

        // Bind Sender Name (shown on received messages)
        if (holder.tvSenderName != null) {
            String senderName = extractSenderDisplayName(message.getSenderId());
            if (MessageConstants.SOS.equals(message.getMessageType())) {
                holder.tvSenderName.setText("🚨 " + senderName);
                holder.tvSenderName.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.sos_red)
                );
            } else {
                holder.tvSenderName.setText(senderName);
                holder.tvSenderName.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.primary)
                );
            }
        }

        // Bind Message Timestamp
        if (holder.tvMessageTime != null) {
            long ts = message.getTimestamp();
            if (ts > 0) {
                holder.tvMessageTime.setText(timeFormat.format(new Date(ts)));
                holder.tvMessageTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvMessageTime.setVisibility(View.GONE);
            }
        }

        // Bind Content & Styling
        if (MessageConstants.SOS.equals(message.getMessageType())) {
            StringBuilder sb = new StringBuilder();
            sb.append("🚨 [EMERGENCY SOS]\n");
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                sb.append(message.getContent());
            }
            if (message.getLatitude() != null && message.getLongitude() != null
                    && (message.getLatitude() != 0.0 || message.getLongitude() != 0.0)) {
                sb.append("\n📍 ").append(
                        String.format(
                                Locale.US,
                                "%.5f, %.5f",
                                message.getLatitude(),
                                message.getLongitude()
                        )
                );
            }

            holder.messageText.setText(sb.toString());
            if (holder.bubbleLayout != null) {
                holder.bubbleLayout.setBackgroundResource(R.drawable.bg_message_sos);
            }
            holder.messageText.setTextColor(Color.WHITE);
            if (holder.tvMessageTime != null) {
                holder.tvMessageTime.setTextColor(Color.parseColor("#FFCDD2"));
            }
        } else {
            holder.messageText.setText(
                    message.getContent()
            );

            if (viewType == VIEW_TYPE_SENT) {
                if (holder.bubbleLayout != null) {
                    holder.bubbleLayout.setBackgroundResource(R.drawable.bg_message_sent);
                }
                holder.messageText.setTextColor(Color.WHITE);
                if (holder.tvMessageTime != null) {
                    holder.tvMessageTime.setTextColor(Color.parseColor("#D1E4FA"));
                }
            } else {
                if (holder.bubbleLayout != null) {
                    holder.bubbleLayout.setBackgroundResource(R.drawable.bg_message_received);
                }
                holder.messageText.setTextColor(Color.BLACK);
                if (holder.tvMessageTime != null) {
                    holder.tvMessageTime.setTextColor(Color.parseColor("#757575"));
                }
            }
        }
    }

    // ---------------------------------------------------------
    // SENDER DISPLAY NAME HELPER
    // ---------------------------------------------------------

    public static String extractSenderDisplayName(String senderId) {
        if (senderId == null || senderId.trim().isEmpty()) {
            return "Nearby Responder";
        }

        String cleaned = senderId.trim();

        // Format: "Name::deviceId"
        if (cleaned.contains("::")) {
            String name = cleaned.substring(0, cleaned.indexOf("::")).trim();
            if (!name.isEmpty()) {
                return name;
            }
        }

        // Format: "Name|deviceId"
        if (cleaned.contains("|")) {
            String name = cleaned.substring(0, cleaned.indexOf("|")).trim();
            if (!name.isEmpty()) {
                return name;
            }
        }

        // If it's a raw UUID (32+ chars with dashes), display friendly identifier
        if (cleaned.length() >= 32 && cleaned.contains("-")) {
            return "User (" + cleaned.substring(0, 6) + ")";
        }

        return cleaned;
    }

    // ---------------------------------------------------------
    // ITEM COUNT
    // ---------------------------------------------------------

    @Override
    public int getItemCount() {

        return messages.size();
    }

    // ---------------------------------------------------------
    // VIEW HOLDER
    // ---------------------------------------------------------

    static class MessageViewHolder
            extends RecyclerView.ViewHolder {

        final TextView messageText;
        final TextView tvSenderName;
        final TextView tvMessageTime;
        final View bubbleLayout;

        MessageViewHolder(
                @NonNull View itemView) {

            super(itemView);

            messageText =
                    itemView.findViewById(
                            R.id.messageText
                    );

            tvSenderName =
                    itemView.findViewById(
                            R.id.tvSenderName
                    );

            tvMessageTime =
                    itemView.findViewById(
                            R.id.tvMessageTime
                    );

            bubbleLayout =
                    itemView.findViewById(
                            R.id.bubbleLayout
                    );
        }
    }
}