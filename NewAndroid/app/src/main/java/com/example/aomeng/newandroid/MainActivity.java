package com.example.aomeng.newandroid;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.services.lexrts.model.GenericAttachment;
import com.amazonaws.regions.Regions;

import com.prakritibansal.posttextrequest.Continuations.LexServiceContinuation;
import com.prakritibansal.posttextrequest.InteractionClient;
import com.prakritibansal.posttextrequest.Listeners.InteractionListener;
import com.prakritibansal.posttextrequest.TextResponse;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private Context appContext;
    private EditText userTextInput;
    private InteractionClient lexInteractionClient;
    public LexServiceContinuation convContinuation;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        init();
    }
    private void init() {
        appContext = getApplicationContext();
        userTextInput = (EditText) findViewById(R.id.userInputEditText);

        initializeLexSDK();
        startNewConversation();
    }

    /**
     * Initializes Lex client.
     */
    private void initializeLexSDK() {
        // Cognito Identity Broker is the credentials provider.
        CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(
                appContext.getResources().getString(R.string.identity_id_test),
                Regions.fromName("us-east-1"));

        // Create Lex interaction client.
        lexInteractionClient = new InteractionClient(getApplicationContext(),
                credentialsProvider,
                Regions.US_EAST_1,
                appContext.getResources().getString(R.string.bot_name),
                appContext.getResources().getString(R.string.bot_alias));
        lexInteractionClient.setInteractionListener(interactionListener);

    }
    final InteractionListener interactionListener = new InteractionListener() {
        @Override
        public void onReadyForFulfillment(final TextResponse response) {

/*
            if(response.getTextResponse() != null){

                Map<String, String> responses = ssmlBreakPoint(response.getTextResponse());

                bot_resp.setText(plainText);
                setupPlayButton(responses.get(SSML));
            }
            if(response.getResponseCard()!= null) {
                GenericAttachment ga = response.getResponseCard().getGenericAttachments().get(0);
                //display response cards
            }

            inConversation = false;
            lexIsResponding = false;
            dotsLoading.setVisibility(LinearLayout.GONE);
            userInp.setVisibility(LinearLayout.VISIBLE);
            requestFocus();
*/

        }
        @Override
        public void promptUserToRespond(final TextResponse response,
                                        final LexServiceContinuation continuation) {
            MainActivity.this.convContinuation = continuation;
    /*
            if(response.getDialogState().equals("Fulfilled")){
                if(response.getTextResponse() != null){
                    Map<String, String> responses = ssmlBreakPoint(response.getTextResponse());
                    bot_resp.setText(plainText);
                    setupPlayButton(responses.get(SSML));
                }
                inConversation = false;
                lexIsResponding = false;
                dotsLoading.setVisibility(LinearLayout.GONE);
                userInp.setVisibility(LinearLayout.VISIBLE);
                requestFocus();
            }else{


                if(response.getTextResponse() != null){

                    Map<String, String> responses = ssmlBreakPoint(response.getTextResponse());
                    bot_resp.setText(plainText);
                    setupPlayButton(responses.get(SSML));
                }
                if(response.getResponseCard()!= null){
                    GenericAttachment ga = response.getResponseCard().getGenericAttachments().get(0);
                    //Display response cards

                }

                readUserText(continuation);
            }*/


        }
        @Override
        public void onInteractionError(final TextResponse response, final Exception e) {

        }
    };

}
