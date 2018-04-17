package com.rmit.aws.chatbot.requestbot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.StrictMode;
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
import android.widget.ScrollView;
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
import com.amazonaws.services.lexrts.AmazonLexRuntimeClient;
import com.amazonaws.services.lexrts.model.PostTextRequest;
import com.amazonaws.services.lexrts.model.PostTextResult;
import com.amazonaws.services.lexrts.model.ResponseCard;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static int PERMISSION_REQ = 1;

    private InteractionClient client;
    private LexServiceContinuation continuation;

    private LinearLayout chatBox;
    private EditText inputText;
    private Button sendButton;
    private Button clearhisoryButton;
    private Button connectptvButton;
    private AmazonLexRuntimeClient clientlr;
    private PostTextRequest postTextRequest;
    private ScrollView scrollView;

    private int userChatLayout = R.layout.user_chat_box;
    private int lexChatLayout = R.layout.lex_chat_box;
    private String buttonvalue;

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
        scrollView=(ScrollView)findViewById(R.id.scrollView1);

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
        receiveMessage("Welcome to use PTV BOT\n" +
                "We can provide you with all the information you need.\n" +
                "You can enter all your questions in the input box");

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
        clientlr=new AmazonLexRuntimeClient(credentialsProvider);
        postTextRequest=new PostTextRequest();
        postTextRequest.setBotAlias(getString(R.string.bot_alias));
        postTextRequest.setBotName(getString(R.string.bot_name));
        postTextRequest.setUserId(getString(R.string.pool_id));
    }

    private void sendMessage(String message) {
        if (message == null || message.isEmpty())
            return;

        if (inConversation) {

            if (continuation == null) {

            }
            continuation.continueWithTextInForTextOut(message);
        } else {
            //client.textInForTextOut(message, null);

        }
        ConstraintLayout userMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.user_chat_box, null);
        TextView messageText = (TextView) userMessageLayout.findViewById(R.id.messege_text);
        messageText.setBackgroundResource(R.drawable.text_view_request);
        messageText.setText(message);
        chatBox.addView(userMessageLayout,0);

        postTextRequest.setInputText(message);
        // PostTextResult result=clientlr.postText(postTextRequest);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        PostTextResult postTextResult=clientlr.postText(postTextRequest);
        String resultText=postTextResult.getMessage();

        if(postTextResult.getResponseCard()!=null){
            ResponseCard responseCard=postTextResult.getResponseCard();
            ConstraintLayout ResponseCardLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lexcard_chat_box, null);
            TextView responsecardText = (TextView) ResponseCardLayout.findViewById(R.id.card_messege_text);
            responsecardText.setText(resultText);
            LinearLayout linearLayout=(LinearLayout)ResponseCardLayout.findViewById(R.id.buttonlayout);
            List<com.amazonaws.services.lexrts.model.Button> buttons=responseCard.getGenericAttachments().get(0).getButtons();
            for(int i=0;i<buttons.size()||i<1;i++){
                Button button=new Button(this);
                button.setText(buttons.get(i).getValue());
                buttonvalue=buttons.get(i).getValue();
                button.setTag(buttonvalue);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String s=(String) v.getTag();
                        sendMessage(s);
                    }
                });
                linearLayout.addView(button);
            }
            chatBox.addView(ResponseCardLayout,0);
        }else {
            ConstraintLayout lexMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lex_chat_box, null);
            TextView messageTextre = (TextView) lexMessageLayout.findViewById(R.id.messege_text);
            messageTextre.setBackgroundResource(R.drawable.text_view_border);
            messageTextre.setText(resultText);
            chatBox.addView(lexMessageLayout,0);
        }
        //scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void receiveMessage(Response response) {
        ConstraintLayout lexMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lex_chat_box, null);
        TextView messageText = (TextView) lexMessageLayout.findViewById(R.id.messege_text);
        messageText.setText(response.getTextResponse());
        chatBox.addView(lexMessageLayout,0);
    }
    private void receiveMessage(String response) {
        ConstraintLayout lexMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lex_chat_box, null);
        TextView messageText = (TextView) lexMessageLayout.findViewById(R.id.messege_text);
        messageText.setBackgroundResource(R.drawable.text_view_border);
        messageText.setText(response);
        chatBox.addView(lexMessageLayout,0);
    }

    private class LexListener implements InteractionListener {


        @Override
        public void onReadyForFulfillment(Response response) {
            continuation = null;
            inConversation = false;
            response.getAudioResponse();
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
