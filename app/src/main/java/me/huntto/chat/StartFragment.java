package me.huntto.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import me.huntto.chat.utils.NetworkUtil;


public class StartFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    private EditText mIpEdit;
    private EditText mPortEdit;
    private ChatSession mChatSession;
    private LinearLayout mOptionsLayout;
    private LinearLayout mIpPortLayout;
    private Button mConnectBtn;

    private ChatSession.OnChatListener mOnChatListener = new ChatSession.OnChatListener() {
        @Override
        public void onConnected() {
            Toast.makeText(getContext(), "Connected", Toast.LENGTH_LONG).show();
            goToChat(mChatSession);
        }

        @Override
        public void onConnectFailed() {
            Toast.makeText(getContext(), "Connect failed" , Toast.LENGTH_LONG).show();
        }

        @Override
        public void onAccepted() {
            Toast.makeText(getContext(), "Accepted", Toast.LENGTH_LONG).show();
            goToChat(mChatSession);
        }

        @Override
        public void onAcceptFailed() {
            Toast.makeText(getContext(), "Accept failed", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onSendMessageSuccess(String msg) {
        }

        @Override
        public void onSendMessageFailed(String msg) {
        }

        @Override
        public void onReceiveMessage(String msg) {
        }
    };

    private void createServer() {
        if (mChatSession != null) {
            mChatSession.release();
        }
        mChatSession = new ChatSession(true, NetworkUtil.getIP(), 0);
        mChatSession.setOnChatListener(mOnChatListener);
        mChatSession.init();
        showServer();
    }

    private void showServer() {
        mOptionsLayout.setVisibility(View.GONE);
        mIpPortLayout.setVisibility(View.VISIBLE);
        mConnectBtn.setVisibility(View.INVISIBLE);
        mPortEdit.setText(String.valueOf(mChatSession.getPort()));
        mIpEdit.setText(mChatSession.getIP());
        mIpEdit.setEnabled(false);
        mPortEdit.setEnabled(false);
    }

    private void connectToServer() {
        String ip = mIpEdit.getText().toString();
        int port = Integer.valueOf(mPortEdit.getText().toString());
        if (checkIpAddress(ip) && checkPort(port)) {
            if (mChatSession != null) {
                mChatSession.release();
            }
            mChatSession = new ChatSession(false, ip, port);
            mChatSession.setOnChatListener(mOnChatListener);
            mChatSession.init();
        }
    }

    private boolean checkPort(int port) {
        // TODO
        return true;
    }

    private boolean checkIpAddress(String ip) {
        // TODO
        return true;
    }

    private void goToChat(ChatSession session) {
        if (mListener != null) {
            mListener.onGoToChat(session);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_start, container, false);
        initViews(contentView);
        return contentView;
    }

    @SuppressLint("SetTextI18n")
    private void initViews(View contentView) {
        mIpPortLayout = contentView.findViewById(R.id.ip_port_layout);
        mOptionsLayout = contentView.findViewById(R.id.options_layout);

        contentView.findViewById(R.id.create_server).setOnClickListener(mOnClickListener);
        contentView.findViewById(R.id.create_client).setOnClickListener(mOnClickListener);

        mIpEdit = contentView.findViewById(R.id.ip_edit);
        mPortEdit = contentView.findViewById(R.id.port_edit);
        mConnectBtn = contentView.findViewById(R.id.connect_to_server);
        mConnectBtn.setOnClickListener(mOnClickListener);
        showOptions();
    }

    private void showOptions() {
        mOptionsLayout.setVisibility(View.VISIBLE);
        mIpPortLayout.setVisibility(View.GONE);
    }


    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.create_server:
                    createServer();
                    break;
                case R.id.create_client:
                    showConnectServer();
                    break;
                case R.id.connect_to_server:
                    connectToServer();
                    break;
            }
        }
    };

    private void showConnectServer() {
        mIpPortLayout.setVisibility(View.VISIBLE);
        mOptionsLayout.setVisibility(View.GONE);
        mConnectBtn.setVisibility(View.VISIBLE);
        if (mIpEdit.getText().toString().isEmpty()) {
            mIpEdit.setText(NetworkUtil.getIP());
        }
        if (mPortEdit.getText().toString().isEmpty()) {
            mPortEdit.setText(NetworkUtil.getDefaultPort());
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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onGoToChat(ChatSession session);
    }
}
