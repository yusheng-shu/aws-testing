package com.rmit.aws.chatbot.requestbot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import com.amazonaws.services.lexrts.model.ResponseCard;

public class MainActivity extends AppCompatActivity {
    private static int PERMISSION_REQ = 1;

    private InteractionClient client;
    private LexServiceContinuation continuation;

    private LinearLayout chatBox;
    private EditText inputText;
    private Button sendButton;
    private Button clearhisoryButton;
    private Button connectptvButton;

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
        clearhisoryButton=(Button) findViewById(R.id.clearhistory);
        connectptvButton=(Button) findViewById(R.id.connectptv);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(inputText.getText().toString());
                inputText.setText("");
            }
        });
        clearhisoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatBox.removeAllViews();
            }
        });
        connectptvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri content_url = Uri.parse("https://www.ptv.vic.gov.au/");
                intent.setData(content_url);
                startActivity(intent);
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
        if(chatBox.getChildCount()>6){
            chatBox.removeViewAt(0);
        }
        chatBox.addView(userMessageLayout);
    }

    private void receiveMessage(Response response) {
        ConstraintLayout lexMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lex_chat_box, null);
        TextView messageText = (TextView) lexMessageLayout.findViewById(R.id.messege_text);
        messageText.setText(response.getTextResponse());
        if(chatBox.getChildCount()>6){
            chatBox.removeViewAt(0);
        }
        chatBox.addView(lexMessageLayout);
    }
    private void receiveMessage(ResponseCard responseCard) {
        ConstraintLayout lexMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lex_chat_box, null);
        TextView messageText = (TextView) lexMessageLayout.findViewById(R.id.messege_text);
        messageText.setText(responseCard.getGenericAttachments().get(0).getImageUrl());
        if(chatBox.getChildCount()>6){
            chatBox.removeViewAt(0);
        }
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
