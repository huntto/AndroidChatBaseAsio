package me.huntto.chat;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;

import java.util.EmptyStackException;
import java.util.Stack;

public class MainActivity extends AppCompatActivity
        implements StartFragment.OnFragmentInteractionListener, ChatFragment.OnFragmentInteractionListener {
    private StartFragment mStartFragment;
    private ChatFragment mChatFragment;
    private Stack<Fragment> mFragmentStack = new Stack<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initFragments();
        showFragment(mStartFragment);
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
        addFragmentToStack(fragment);
    }

    private void addFragmentToStack(Fragment fragment) {
        if (!mFragmentStack.contains(fragment)) {
            mFragmentStack.add(fragment);
        }
    }

    private void initFragments() {
        mStartFragment = new StartFragment();
        mChatFragment = new ChatFragment();
    }

    @Override
    public void onGoToChat(ChatSession session) {
        mChatFragment.setChatSession(session);
        showFragment(mChatFragment);
    }

    @Override
    public void onBackToStart() {
        showFragment(mStartFragment);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK && popFragmentFromStack() || super.onKeyDown(keyCode, event);
    }

    private boolean popFragmentFromStack() {
        try {
            mFragmentStack.pop();
            Fragment fragment = mFragmentStack.peek();
            if (fragment != null) {
                showFragment(mFragmentStack.peek());
                return true;
            }
            return false;
        } catch (EmptyStackException e) {
            return false;
        }
    }
}
