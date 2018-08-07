package me.huntto.chat;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.huntto.chat.utils.LogUtil;

class Message {
    private static final String SPLITTER = ":";
    private static int sId = 0x01;
    final int id;
    final boolean host;
    final String content;

    Message(boolean host, String content) {
        this.id = sId++;
        this.host = host;
        this.content = content;
    }

    public String toNativeMsg() {
        return String.valueOf(id) +
                SPLITTER +
                content;
    }

    static Message fromNativeMsg(String nativeMsg) {
        String[] splits = nativeMsg.trim().split(SPLITTER);
        LogUtil.e(nativeMsg);
        if (splits.length == 2) {
            return new Message(false, splits[1]);
        }
        return null;
    }

}

public class ChatFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    private ChatSession mChatSession;
    private MsgListAdapter mMsgListAdapter;
    private EditText mMsgEdit;


    private List<Message> mMsgList = new ArrayList<>();

    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_chat, container, false);
        initViews(contentView);
        return contentView;
    }

    private void initViews(View contentView) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        RecyclerView msgListView = contentView.findViewById(R.id.msg_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setReverseLayout(true);
        layoutManager.setOrientation(OrientationHelper.VERTICAL);
        msgListView.setLayoutManager(layoutManager);
        mMsgList.clear();
        mMsgListAdapter = new MsgListAdapter(mMsgList);
        msgListView.setAdapter(mMsgListAdapter);
        msgListView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL));
        msgListView.setItemAnimator(new DefaultItemAnimator());

        contentView.findViewById(R.id.send_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSendMsg();
            }
        });
        mMsgEdit = contentView.findViewById(R.id.msg_edit);
    }

    private void triggerSendMsg() {
        String input = mMsgEdit.getText().toString();
        if (input.isEmpty()) {
            return;
        }

        Message msg = new Message(true, input);

        if (mChatSession != null) {
            mChatSession.sendMessage(msg.toNativeMsg());
        }

        mMsgList.add(0, msg);
        mMsgListAdapter.notifyDataSetChanged();
        mMsgEdit.setText("");
    }

    public class MsgListAdapter extends RecyclerView.Adapter<MsgListAdapter.MsgViewHolder> {

        class MsgViewHolder extends RecyclerView.ViewHolder {
            final LinearLayout otherMsgLayout;
            final LinearLayout myMsgLayout;
            final TextView otherMsgVew;
            final TextView myMsgView;

            MsgViewHolder(View itemView) {
                super(itemView);
                otherMsgLayout = itemView.findViewById(R.id.other_msg_layout);
                myMsgLayout = itemView.findViewById(R.id.my_msg_layout);
                otherMsgVew = itemView.findViewById(R.id.other_msg_view);
                myMsgView = itemView.findViewById(R.id.my_msg_view);
            }
        }

        private List<Message> mMsgList;

        public MsgListAdapter(List<Message> msgList) {
            this.mMsgList = msgList;
        }

        @Override
        public void onBindViewHolder(@NonNull MsgViewHolder holder, int position) {
            Message msg = mMsgList.get(position);
            LogUtil.i(msg.content);
            if (msg.host) {
                holder.otherMsgLayout.setVisibility(View.GONE);
                holder.myMsgView.setText(msg.content);
                holder.myMsgLayout.setVisibility(View.VISIBLE);
            } else {
                holder.myMsgLayout.setVisibility(View.GONE);
                holder.otherMsgVew.setText(msg.content);
                holder.otherMsgLayout.setVisibility(View.VISIBLE);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }

        @Override
        public int getItemCount() {
            return mMsgList.size();
        }

        @NonNull
        @Override
        public MsgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_msg, parent, false);
            return new MsgViewHolder(itemView);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    private ChatSession.OnChatListener mOnChatListener = new ChatSession.OnChatListener() {
        @Override
        public void onConnected() {

        }

        @Override
        public void onConnectFailed() {

        }

        @Override
        public void onAccepted() {

        }

        @Override
        public void onAcceptFailed() {

        }

        @Override
        public void onSendMessageSuccess(String msg) {
            LogUtil.e("Send \"" + msg + "\" success");
        }

        @Override
        public void onSendMessageFailed(String msg) {
            Toast.makeText(getContext(), "Send \"" + msg + "\" failed", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onReceiveMessage(String msg) {
            Message message = Message.fromNativeMsg(msg);
            if (message != null) {
                mMsgList.add(0, message);
                mMsgListAdapter.notifyDataSetChanged();
            } else {
                LogUtil.e("null msg");
            }
        }
    };

    public void setChatSession(ChatSession session) {
        mChatSession = session;
        mChatSession.setOnChatListener(mOnChatListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (mChatSession != null) {
            mChatSession.release();
            mChatSession = null;
        }
    }

    public interface OnFragmentInteractionListener {
        void onBackToStart();
    }
}
