package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class SimpleDhtActivity extends Activity {

    //static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};
    //static final int SERVER_PORT = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);

        final Uri uriobject=buildUri("content","edu.buffalo.cse.cse486586.simpledht.provider");

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(new OnTestClickListener(tv, getContentResolver()));

        final Button button1 = (Button) findViewById(R.id.button1);
        final Button button2 = (Button) findViewById(R.id.button2);
        final Button button3 = (Button) findViewById(R.id.button3);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Cursor cur=getContentResolver().query(uriobject,null,"@",null,null);
                Log.d("test","Key in matcursor, key: "+cur.getColumnName(0)+" Value: "+cur.getColumnName(1));
                int i=0;
                int count=cur.getCount();
                tv.setText("");
                tv.append("Cursor matrix size: "+count+"\n");
                while(i<count){
                    cur.moveToNext();
                    String key=cur.getString(0);
                    String value=cur.getString(1);
                    Log.d("test","iteration,i: "+i+" Key: "+key+" Value: "+value);
                    tv.append("Key: "+key+" Value: "+value+"\n");
                    i++;
                }
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Cursor cur=getContentResolver().query(uriobject,null,"*",null,null);
                Log.d("test","Key in matcursor, key: "+cur.getColumnName(0)+" Value: "+cur.getColumnName(1));
                int i=0;
                int count=cur.getCount();
                tv.setText("");
                tv.append("Cursor matrix size: "+count+"\n");
                while(i<count){
                    cur.moveToNext();
                    String key=cur.getString(0);
                    String value=cur.getString(1);
                    Log.d("test","iteration,i: "+i+" Key: "+key+" Value: "+value);
                    tv.append("Key: "+key+" Value: "+value+"\n");
                    i++;
                }
            }
        });

    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}