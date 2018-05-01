package com.rmit.aws.chatbot.requestbot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Parcelable;
import android.os.StrictMode;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.InteractionClient;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.continuations.LexServiceContinuation;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.InteractionListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lexrts.AmazonLexRuntimeClient;
import com.amazonaws.services.lexrts.model.PostTextRequest;
import com.amazonaws.services.lexrts.model.PostTextResult;
import com.amazonaws.services.lexrts.model.ResponseCard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private String transfermessage="";
    private String murl="";

    private boolean inConversation = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent;
        switch (item.getItemId()){
            case R.id.connectptv_item:
                intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri content_url = Uri.parse("https://www.ptv.vic.gov.au/");
                intent.setData(content_url);
                startActivity(intent);
                break;
        }
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("测试测asdadads");
        initPermission();
        System.out.println("测试测asdadads");
        initLex();

        chatBox = (LinearLayout) findViewById(R.id.chat_box);
        inputText = (EditText) findViewById(R.id.input_text);
        sendButton = (Button) findViewById(R.id.send_button);
        clearhisoryButton=(Button) findViewById(R.id.clear_history);
        scrollView=(ScrollView)findViewById(R.id.scroll_view);

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
        receiveMessage("Welcome to use PTV BOT\n" +
                "We can provide you with all the information you need.\n" +
                "You can enter all your questions in the input box");
        System.out.println("测试测asdadads");
    }

    private void initPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET} , PERMISSION_REQ);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE} , PERMISSION_REQ);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQ);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQ);
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
            client.textInForTextOut(message, null);

        }
        ConstraintLayout userMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.user_chat_box, null);
        TextView messageText = (TextView) userMessageLayout.findViewById(R.id.messege_text);
        messageText.setBackgroundResource(R.drawable.text_view_request);
        Location location=getLocation();
        messageText.setText(message+"("+location.getLongitude()+")");
        chatBox.addView(userMessageLayout);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                // scrollView.scrollTo(0, scrollView.getBottom()); // This might also work for smooth scrolling
            }
        });
        transfermessage = message;

        if(location!=null){
            Map sessionAttributes=new HashMap();
            sessionAttributes.put("latitude",location.getLatitude()+"");
            sessionAttributes.put("longitude",location.getLongitude()+"");
            postTextRequest.setSessionAttributes(sessionAttributes);}

    }
    private void receiveMessage() {
        postTextRequest.setInputText(transfermessage);
        // PostTextResult result=clientlr.postText(postTextRequest);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // TODO ASYNC Put this line of code in an AsyncTask class ================================================================================================================================

        PostTextResult postTextResult=clientlr.postText(postTextRequest);

        // TODO ASYNC Put this line of code in an AsyncTask class ================================================================================================================================

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
            chatBox.addView(ResponseCardLayout);
        }else if (resultText.equals("Sorry, I can't help you with that. Would you like to be transfered to a customer service rep?")) {
            ConstraintLayout ResponseCardLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lexcard_chat_box, null);
            TextView responsecardText = (TextView) ResponseCardLayout.findViewById(R.id.card_messege_text);
            responsecardText.setText(resultText);
            LinearLayout linearLayout = (LinearLayout) ResponseCardLayout.findViewById(R.id.buttonlayout);

            Button button = new Button(this);
            button.setText("Call PTV");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + "1800800007"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
            linearLayout.addView(button);
            chatBox.addView(ResponseCardLayout);
        } else  {
            ConstraintLayout lexMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lex_chat_box, null);
            TextView messageTextre = (TextView) lexMessageLayout.findViewById(R.id.messege_text);
            messageTextre.setBackgroundResource(R.drawable.text_view_border);
            //TRY SOME
            Pattern p=Pattern.compile(".*(tinyurl\\.com\\/[0-9a-z-]+)");
            Matcher m=p.matcher(resultText.toString());
            boolean b=m.matches();
            //TRY END
            if(b){
                resultText=resultText.replace(m.group(1),"");
                messageTextre.setText(resultText);
                SpannableString spString = new SpannableString(m.group(1));
                spString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        String s=((TextView)widget).getText().toString();
                        Pattern pss=Pattern.compile(".*(tinyurl\\.com\\/[0-9a-z-]+)");
                        Matcher mss=pss.matcher(s);
                        mss.matches();
                        Uri content_url = Uri.parse("https://"+mss.group(1));
                        intent.setData(content_url);
                        startActivity(intent);
                    }
                }, 0, m.group(1).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageTextre.append(spString);
                messageTextre.setMovementMethod(LinkMovementMethod.getInstance());
            }else{
            messageTextre.setText(resultText);
            }
            chatBox.addView(lexMessageLayout);
        }

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                // scrollView.scrollTo(0, scrollView.getBottom()); // This might also work for smooth scrolling
            }
        });
        //chatBox.addView(lexMessageLayout);
    }
    private void receiveMessage(String response) {
        ConstraintLayout lexMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.lex_chat_box, null);
        TextView messageText = (TextView) lexMessageLayout.findViewById(R.id.messege_text);
        messageText.setBackgroundResource(R.drawable.text_view_border);
        messageText.setText(response);
        chatBox.addView(lexMessageLayout);
    }

    private class LexListener implements InteractionListener {


        @Override
        public void onReadyForFulfillment(Response response) {
            continuation = null;
            inConversation = false;
            response.getAudioResponse();
            receiveMessage();
        }

        @Override
        public void promptUserToRespond(Response response, LexServiceContinuation continuation) {
            MainActivity.this.continuation = continuation;
            receiveMessage();
        }

        @Override
        public void onInteractionError(Response response, Exception e) {
            continuation = null;
            inConversation = false;
            receiveMessage();
        }
    }
    private Location getLocation() {

        String provider = LocationManager.NETWORK_PROVIDER;
        String serviceString = Context.LOCATION_SERVICE;
        LocationManager locationManager = (LocationManager) getSystemService(serviceString);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        Location location = locationManager.getLastKnownLocation(provider);
        if(location==null){
            provider = LocationManager.GPS_PROVIDER;
            serviceString = Context.LOCATION_SERVICE;
            locationManager = (LocationManager) getSystemService(serviceString);
            location = locationManager.getLastKnownLocation(provider);
        }
        //System.out.println(location.getLatitude()+","+location.getLongitude());
        return location;
    }
}
