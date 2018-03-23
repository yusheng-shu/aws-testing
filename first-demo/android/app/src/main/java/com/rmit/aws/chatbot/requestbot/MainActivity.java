package com.rmit.aws.chatbot.requestbot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.lex.interactionkit.InteractionClient;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.continuations.LexServiceContinuation;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.InteractionListener;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;
import com.amazonaws.regions.Regions;

public class MainActivity extends AppCompatActivity {
    private static int PERMISSION_REQ = 1;

    private InteractionClient client;
    private LexServiceContinuation continuation;

    private LinearLayout chatBox;
    private EditText inputText;
    private Button sendButton;

    private int userChatLayout = R.layout.user_chat_box;
    private int lexChatLayout = R.layout.lex_chat_box;

    private boolean inConversation = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPermission();

        initLex();

        chatBox = (LinearLayout) findViewById(R.id.chat_box);
        inputText = (EditText) findViewById(R.id.input_text);
        sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(inputText.getText().toString());
                inputText.setText("");
            }
        });

    }

    private void initPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET} , PERMISSION_REQ);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE} , PERMISSION_REQ);
        }
    }

    private void initLex() {
        CognitoCredentialsProvider credentialsProvider =  new CognitoCredentialsProvider(
                getString(R.string.pool_id),
                Regions.fromName(getString(R.string.region)));
        InteractionConfig config = new InteractionConfig(
                getString(R.string.bot_name),
                getString(R.string.bot_alias));
        client = new InteractionClient(
                this,
                credentialsProvider,
                Regions.fromName(getString(R.string.region)),
                config);
        client.setInteractionListener(new LexListener());
    }

    private void sendMessage(String message) {
        if (message == null || message.isEmpty())
            return;

        if (inConversation) {

            if (continuation == null) {

            }
            continuation.continueWithTextInForTextOut(message);
        } else {

            client.textInForTextOut(message, null);
        }

        ConstraintLayout userMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.user_chat_box, null);
        TextView messageText = (TextView) userMessageLayout.findViewById(R.id.messege_text);
        messageText.setText(message);
        chatBox.addView(userMessageLayout);
    }

    private void receiveMessage(Response response) {
        ConstraintLayout lexMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lex_chat_box, null);
        TextView messageText = (TextView) lexMessageLayout.findViewById(R.id.messege_text);
        messageText.setText(response.getTextResponse());
        chatBox.addView(lexMessageLayout);
    }

    private class LexListener implements InteractionListener {

        @Override
        public void onReadyForFulfillment(Response response) {
            continuation = null;
            inConversation = false;
            receiveMessage(response);
        }

        @Override
        public void promptUserToRespond(Response response, LexServiceContinuation continuation) {
            MainActivity.this.continuation = continuation;
            receiveMessage(response);
        }

        @Override
        public void onInteractionError(Response response, Exception e) {
            continuation = null;
            inConversation = false;
            receiveMessage(response);
        }
    }
}
