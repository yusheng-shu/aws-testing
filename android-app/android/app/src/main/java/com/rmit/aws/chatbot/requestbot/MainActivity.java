package com.rmit.aws.chatbot.requestbot;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.os.StrictMode;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.AudioPlaybackListener;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.InteractionListener;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lexrts.AmazonLexRuntimeClient;
import com.amazonaws.services.lexrts.model.PostTextRequest;
import com.amazonaws.services.lexrts.model.PostTextResult;
import com.amazonaws.services.lexrts.model.ResponseCard;

import java.util.ArrayList;
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
    public AmazonLexRuntimeClient clientlr;
    private PostTextRequest postTextRequest;
    private ScrollView scrollView;
    private InteractiveVoiceView interactiveVoiceView;
    private LexAudioListener lexAudioListener=new LexAudioListener();
    private CognitoCredentialsProvider credentialsProvider;

    private int userChatLayout = R.layout.user_chat_box;
    private int lexChatLayout = R.layout.lex_chat_box;
    private String buttonvalue;
    private String transfermessage="";
    private String murl="";
    private String ssurl="";

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
        initLex();
        if(isOpenLocService(this)){;}else {
            new AlertDialog.Builder(this)
                    .setTitle("Undetected GPS service")
                    .setMessage("you should open GPS service")
                    .setNegativeButton("sure", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            gotoLocServiceSettings(MainActivity.this);
                        }
                    })
                    .setPositiveButton("Of course", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            gotoLocServiceSettings(MainActivity.this);
                        }
                    })
                    .create().show();
        }

        chatBox = (LinearLayout) findViewById(R.id.chat_box);
        inputText = (EditText) findViewById(R.id.input_text);
        sendButton = (Button) findViewById(R.id.send_button);
        clearhisoryButton=(Button) findViewById(R.id.clear_history);
        scrollView=(ScrollView)findViewById(R.id.scroll_view);
        interactiveVoiceView=(InteractiveVoiceView)findViewById(R.id.voiceInterface);

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
        System.out.println("测试测asdadads");

        interactiveVoiceView.setInteractiveVoiceListener(new InteractiveVoiceView.InteractiveVoiceListener() {
            @Override
            public void dialogReadyForFulfillment(Map<String, String> slots, String intent) {
                System.out.println("111");
            }

            @Override
            public void onResponse(Response response) {
                System.out.println("222");
                receiveAudioMessage(response);
            }

            @Override
            public void onError(String responseText, Exception e) {
                System.out.println("333");
                return;
            }
        });
        interactiveVoiceView.getViewAdapter().setCredentialProvider(credentialsProvider);
        interactiveVoiceView.getViewAdapter().setInteractionConfig(new InteractionConfig(getString(R.string.bot_name),getString(R.string.bot_alias)));
        interactiveVoiceView.getViewAdapter().setAwsRegion(getString(R.string.region));
    }

    private void initPermission() {
        ArrayList<String> primissions=new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
           primissions.add(Manifest.permission.INTERNET);
           //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET} , PERMISSION_REQ);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_DENIED) {
            primissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE} , PERMISSION_REQ);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            primissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQ);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            primissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQ);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            primissions.add(Manifest.permission.RECORD_AUDIO);
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ);
        }
        if(primissions.size()>0)
        ActivityCompat.requestPermissions(this,primissions.toArray(new String[]{}), PERMISSION_REQ);
    }

    private void initLex() {
        credentialsProvider =  new CognitoCredentialsProvider(
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
        client.setAudioPlaybackListener(lexAudioListener);
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
            //continuation.continueWithTextInForTextOut(message);
        } else {
            //client.textInForTextOut(message, null);

        }
        ConstraintLayout userMessageLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.user_chat_box, null);
        TextView messageText = (TextView) userMessageLayout.findViewById(R.id.messege_text);
        messageText.setBackgroundResource(R.drawable.text_view_request);
        Location location=getLocation();
        messageText.setText(message);
        chatBox.addView(userMessageLayout);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                // scrollView.scrollTo(0, scrollView.getBottom()); // This might also work for smooth scrolling
            }
        });
        transfermessage = message;
        Map sessionAttributes=new HashMap();
        if(location!=null){
            System.out.println(location.getLatitude()+"啊啊啊啊啊");

            sessionAttributes.put("latitude",location.getLatitude()+"");
            sessionAttributes.put("longitude",location.getLongitude()+"");
        }
        postTextRequest.setSessionAttributes(sessionAttributes);
        postTextRequest.setInputText(transfermessage);
        new ReceiveMessageTask(MainActivity.this).execute(postTextRequest);

    }


    public void receiveAudioMessage(Response response){
        String resultText=response.getTextResponse();
        if (resultText.equals("Sorry, I can't help you with that. Would you like to be transfered to a customer service rep?")) {
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
            //TRY SOME
            Pattern p2=Pattern.compile(".*\\s(https:\\/\\/www\\.google\\.com\\S*)");
            Matcher m2=p2.matcher(resultText.toString());
            boolean b2=m2.matches();
            System.out.println(b2+"敖德萨");
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
            }else

            if(b2){
                //Do something
                ssurl=m2.group(1);
                resultText=resultText.replace(ssurl,"");
                messageTextre.setText(resultText);
                String urls="see the Google Map";
                SpannableString spString = new SpannableString(urls);
                spString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        System.out.println("url:"+ssurl);
                        Uri content_url = Uri.parse(ssurl);
                        intent.setData(content_url);
                        startActivity(intent);
                    }
                }, 0, urls.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageTextre.append(spString);
                messageTextre.setMovementMethod(LinkMovementMethod.getInstance());

            }else {
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
    }


    public void receiveMessage(PostTextResult postTextResult) {


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
            //TRY SOME
            Pattern p2=Pattern.compile(".*\\s(https:\\/\\/www\\.google\\.com\\S*)");
            Matcher m2=p2.matcher(resultText.toString());
            boolean b2=m2.matches();
            System.out.println(b2+"敖德萨");
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
            }else

                if(b2){
                //Do something
                    ssurl=m2.group(1);
                resultText=resultText.replace(ssurl,"");
                messageTextre.setText(resultText);
                String urls="see the Google Map";
                SpannableString spString = new SpannableString(urls);
                spString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        System.out.println("url:"+ssurl);
                        Uri content_url = Uri.parse(ssurl);
                        intent.setData(content_url);
                        startActivity(intent);
                    }
                }, 0, urls.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageTextre.append(spString);
                messageTextre.setMovementMethod(LinkMovementMethod.getInstance());

            }else {
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
            //receiveMessage();
        }

        @Override
        public void promptUserToRespond(Response response, LexServiceContinuation continuation) {
            MainActivity.this.continuation = continuation;
            //receiveMessage();
        }

        @Override
        public void onInteractionError(Response response, Exception e) {
            continuation = null;
            inConversation = false;
            //receiveMessage();
        }
    }
    private class LexAudioListener implements AudioPlaybackListener{

        @Override
        public void onAudioPlaybackStarted() {

        }

        @Override
        public void onAudioPlayBackCompleted() {
        }

        @Override
        public void onAudioPlaybackError(Exception e) {

        }
    }

    private Location getLocation() {

        String provider = LocationManager.GPS_PROVIDER;
        String serviceString = Context.LOCATION_SERVICE;
        LocationManager locationManager = (LocationManager) getSystemService(serviceString);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("primission problems");
            return null;
        }
        Location location = locationManager.getLastKnownLocation(provider);
        if(location==null){
            provider = LocationManager.NETWORK_PROVIDER;
            serviceString = Context.LOCATION_SERVICE;
            locationManager = (LocationManager) getSystemService(serviceString);
            location = locationManager.getLastKnownLocation(provider);
        }
        if(location!=null) {
            System.out.println(location.getLatitude() + "," + location.getLongitude());
        }else{
            System.out.println("can not get location");
        }
            return location;
    }


    public boolean isOpenLocService(Context context) {
        boolean isGps = false; //判断GPS定位是否启动
        boolean isNetwork = false; //判断网络定位是否启动
            LocationManager locationManager
                    = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                //通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
                isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                //通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
                isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
            if (isGps || isNetwork) {
                return true;
            }
        return false;
    }
    public void gotoLocServiceSettings(Context context) {
        final Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    public void locationserveron(Context context){
        if(isOpenLocService(context)){
            ;
        }else{
            gotoLocServiceSettings(context);
        }
    }
}
