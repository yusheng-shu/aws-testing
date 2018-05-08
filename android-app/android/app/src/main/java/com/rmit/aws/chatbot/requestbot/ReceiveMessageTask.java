package com.rmit.aws.chatbot.requestbot;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.constraint.ConstraintLayout;

import com.amazonaws.services.lexrts.model.PostTextRequest;
import com.amazonaws.services.lexrts.model.PostTextResult;
import com.amazonaws.services.lexrts.model.ResponseCard;
import com.rmit.aws.chatbot.requestbot.MainActivity;

public class ReceiveMessageTask extends AsyncTask<PostTextRequest,Void,PostTextResult> {
    private MainActivity thisAtivity;
    public ReceiveMessageTask(MainActivity mainActivity){
        thisAtivity=mainActivity;
    }
    @Override
    protected PostTextResult doInBackground(PostTextRequest... params) {
        PostTextRequest postTextRequest=params[0];
        PostTextResult postTextResult=thisAtivity.clientlr.postText(postTextRequest);
        return postTextResult;
    }

    @Override
    protected void onPostExecute(PostTextResult postTextResult) {
        super.onPostExecute(postTextResult);
        thisAtivity.receiveMessage(postTextResult);
    }
}
