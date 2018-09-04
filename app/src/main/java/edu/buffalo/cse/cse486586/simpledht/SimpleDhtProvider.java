package edu.buffalo.cse.cse486586.simpledht;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;


/*

References:

https://developer.android.com/reference/java/util/concurrent/BlockingQueue.html
https://developer.android.com/guide/topics/providers/content-providers.html
https://developer.android.com/reference/android/database/Cursor.html
https://stackoverflow.com/questions/10723770/whats-the-best-way-to-iterate-an-android-cursor
https://developer.android.com/reference/java/io/File.html
https://developer.android.com/reference/android/content/Context.html#deleteFile(java.lang.String)

*/



public class SimpleDhtProvider extends ContentProvider {
    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    ArrayList<Node> lst = new ArrayList<Node>();
    BlockingQueue<String> blo=new ArrayBlockingQueue<String>(1);
    String gportStr = null;
    String myPort;
    String sendtoport;
    Uri uriobject=buildUri("content","edu.buffalo.cse.cse486586.simpledht.provider");

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        String[] strsplit=selection.split(";");
//        if(lst.size()==1 && strsplit.length==1  && !(strsplit[0].compareTo("@")==0 || strsplit[0].compareTo("*")==0)){
        if(lst.size()==1 && !(strsplit[0].compareTo("@")==0 || strsplit[0].compareTo("*")==0)){
            boolean del=getContext().deleteFile(strsplit[0]);
        }
        else if(strsplit[0].startsWith("@")){
            String[] file = getContext().fileList();
            //Log.d("test","List of files: "+file[0]+" "+file[1]+" "+file[2]+" "+file[3]+" "+file[4]);
            for (int i = 0; i < file.length; i++) {
                boolean del=getContext().deleteFile(file[i]);
            }
        }
        else if(strsplit[0].startsWith("*")){
            String[] file = getContext().fileList();
            //Log.d("test","List of files: "+file[0]+" "+file[1]+" "+file[2]+" "+file[3]+" "+file[4]);
            for (int i = 0; i < file.length; i++) {
                boolean del=getContext().deleteFile(file[i]);
            }
            if(lst.size()!=1 && strsplit.length==1) {
                String delwait;
                for (int i = 0; i < lst.size(); i++) {
                    if (myPort.compareTo(lst.get(i).id) == 0) {
                        continue;
                    }
                    String msg = "delete;*;0";
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, lst.get(i).id);
                    try {
                        delwait = blo.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else{
            if(strsplit.length==1) {
                boolean del = getContext().deleteFile(strsplit[0]);
                if(del==false){
                    Log.d("test","File not found in this avd for deletion");
                    String delwait;
                    for(int i=0;i<lst.size();i++){
                        if(myPort.compareTo(lst.get(i).id)==0){
                            continue;
                        }
                        String msg="delete;0;"+strsplit[0];
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,lst.get(i).id);
                        try {
                            delwait=blo.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            else if(strsplit.length==2 && strsplit[0].compareTo("0")==0){
                boolean del=getContext().deleteFile(strsplit[1]);
                Log.d("test","Testing file deletion on avd myport: "+myPort+" del: "+del);
            }
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        String comp=null;
        try {
            comp=genHash(values.getAsString("key"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Node nobj=null;
        Iterator iter=lst.iterator();
        while(iter.hasNext()){
            nobj= (Node) iter.next();
            if(nobj.hid.compareTo(gportStr)==0){
                break;
            }
        }
        Log.d("test","Entering insert for key:"+ values.getAsString("key")+" value: "+values.getAsString("value")+" myport: "+myPort);
        try{
            if((gportStr.compareTo(lst.get(0).hid)==0 && (comp.compareTo(lst.get(0).hid)<=0 || comp.compareTo(lst.get(lst.size()-1).hid)>0)) || (comp.compareTo(gportStr)<=0 && comp.compareTo(nobj.pred)>0) || lst.size()==1){
                try {
                    FileOutputStream out = getContext().openFileOutput(values.getAsString("key"), Context.MODE_PRIVATE);
                    Log.d("test","Inserting to file for key:"+ values.getAsString("key")+" value: "+values.getAsString("value"));
                    out.write(values.getAsString("value").getBytes());
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                String msg="insert;"+values.getAsString("key")+";"+values.getAsString("value")+"\n";
                Log.d("test","Passing insert to succ for key:"+ values.getAsString("key")+" value: "+values.getAsString("value")+" msg="+msg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            gportStr = genHash(portStr);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Node nobj=new Node();
        nobj.id=myPort;
        nobj.hid=gportStr;
        nobj.succ=nobj.hid;
        nobj.pred=nobj.hid;
        Log.d("test","Adding to arraylist, device: "+nobj.hid+" id: "+nobj.id);
        lst.add(nobj);

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String msg="add;"+myPort+";"+gportStr+"\n";
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // TODO Auto-generated method stub
        String[] strsplit=selection.split(";");
        FileInputStream in;
        Log.d("test","Entering query for key:"+ strsplit[0]+" myport: "+myPort);
        String[] head = {"key", "value"};
        MatrixCursor cur = new MatrixCursor(head);
        if(lst.size()==1 && !(strsplit[0].startsWith("@") || strsplit[0].startsWith("*"))){
            //File[] file=getContext().getFilesDir().listFiles();
            Log.d("test", "Entering query(size==1) for key:" + strsplit[0] + " myport: " + myPort);
            //String[] file = getContext().fileList();
            //Log.d("test","List of files: "+file[0]+" "+file[1]+" "+file[2]+" "+file[3]+" "+file[4]);
            String input = "";
            try {
                StringBuilder bui = new StringBuilder();
                in = getContext().openFileInput(strsplit[0]);
                int eof;
                while ((eof = in.read()) != -1) {
                    bui.append((char) eof);
                }
                input = bui.toString();
                in.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("test", "Query function(" + strsplit[0] + "), Key: " + selection + " Value: " + input);
            cur.addRow(new Object[]{selection, input});
        }
        else if(strsplit[0].startsWith("@")){
            Log.d("test", "Entering query(@) for key:" + strsplit[0] + " myport: " + myPort);
            String[] file = getContext().fileList();
            //Log.d("test","List of files: "+file[0]+" "+file[1]+" "+file[2]+" "+file[3]+" "+file[4]);
            for (int i = 0; i < file.length; i++) {
                String input = "";
                try {
                    StringBuilder bui = new StringBuilder();
                    in = getContext().openFileInput(file[i]);
                    int eof;
                    while ((eof = in.read()) != -1) {
                        bui.append((char) eof);
                    }
                    input = bui.toString();
                    in.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("test", "Query function(" + strsplit[0] + "), Key: " + file[i] + " Value: " + input);
                cur.addRow(new Object[]{file[i], input});
            }
        }
        else if(strsplit[0].startsWith("*")) {
            String[] file = getContext().fileList();
            String waitstar = null;
            //Integer tag = null;
            if(strsplit.length==1 && lst.size()==1){
                for (int i = 0; i < file.length; i++) {
                    String input = "";
                    try {
                        StringBuilder bui = new StringBuilder();
                        in = getContext().openFileInput(file[i]);
                        int eof;
                        while ((eof = in.read()) != -1) {
                            bui.append((char) eof);
                        }
                        input = bui.toString();
                        in.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d("test", "Query function(" + strsplit[0] + "), Key: " + file[i] + " Value: " + input);
                    cur.addRow(new Object[]{file[i], input});
                }
            }
            else if (strsplit.length == 1) {
                Log.d("test","File size in query avd: "+file.length);
                for (int i = 0; i < file.length; i++) {
                    String input = "";
                    try {
                        StringBuilder bui = new StringBuilder();
                        in = getContext().openFileInput(file[i]);
                        int eof;
                        while ((eof = in.read()) != -1) {
                            bui.append((char) eof);
                        }
                        input = bui.toString();
                        in.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d("test", "Query function(" + strsplit[0] + "), Key: " + file[i] + " Value: " + input);
                    cur.addRow(new Object[]{file[i], input});
                }
                for (int i = 0; i < lst.size(); i++) {
                    if (lst.get(i).id.compareTo(myPort) == 0) {
                        continue;
                    }
                    Log.d("test", "Sending query(*) down the list and blocking, myport: " + myPort);
                    String msg = "query;*;0;" + myPort + "\n";
                    sendtoport = lst.get(i).id;
                    Log.d("test","Sending to port: "+sendtoport);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, sendtoport);
                    try {
                        waitstar=blo.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d("test","String waitstar and value: "+waitstar.length()+" value: "+waitstar);
                    String[] strsplit2=waitstar.split(";");
                    for(int itersplit=1;itersplit<strsplit2.length;itersplit+=2){
                        Log.d("test","Adding to cursor matrix from port:"+sendtoport+" key: "+strsplit2[itersplit]+" value: "+strsplit2[itersplit+1]);
                        cur.addRow(new Object[]{strsplit2[itersplit],strsplit2[itersplit+1]});
                    }
                }
                Log.d("test","Size of cursor rows list: "+cur.getCount());
            }
            else if(strsplit.length==3){
                Log.d("test","Sending query(*) back as a combined list, mypot: "+myPort);
                String msg = "query;*;1;"+ strsplit[2];
                Log.d("test","File list size: "+file.length);
                for (int i = 0; i < file.length; i++) {
                    String input = "";
                    Log.d("test","Inside for loop");
                    try {
                        StringBuilder bui = new StringBuilder();
                        in = getContext().openFileInput(file[i]);
                        int eof;
                        while ((eof = in.read()) != -1) {
                            bui.append((char) eof);
                        }
                        input = bui.toString();
                        in.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d("test", "Query function(" + strsplit[0] + "), Adding to string, Key: " + file[i] + " Value: " + input);
                    msg=msg+";"+file[i]+";"+input;
                    //cur.addRow(new Object[]{file[i], input});
                }
                msg=msg+"\n";
                Log.d("test","Sending to ClientTask with msg: "+msg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,strsplit[2]);
            }
        }
        else {
            StringBuilder bui = new StringBuilder();
            String input;
            Log.d("test","Entering query (individual) for key:"+ strsplit[0]+" myport: "+myPort);
            try {
                in = getContext().openFileInput(strsplit[0]);
                int eof;
                while ((eof = in.read()) != -1) {
                    bui.append((char) eof);
                }
                input = bui.toString();
                in.close();
                Log.d("test","Query inside found in same avd as query avd key: "+strsplit[0]+" value: "+input);
                cur.addRow(new Object[]{strsplit[0], input});
                if (strsplit.length == 2 && !strsplit[1].equals(myPort)) {
                    Log.d("test", "Query inside found k-v pair after passing msg on, key: "+strsplit[0]+" value: "+input+" writing back to: "+strsplit[1]);
                    String msg = "query;1;" + strsplit[0] + ";" + input + ";" + strsplit[1] + "\n";
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
                }
            } catch (FileNotFoundException e) {
                if (strsplit.length == 2 && !strsplit[1].equals(myPort)) {
                    Log.d("test","Entering query when not found in this avd key:"+ strsplit[0]+" stringsplit[1]: "+strsplit[1]);
                    String msg = "query;0;" + strsplit[0] + ";" + strsplit[1] + "\n";
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
                }
                else {
                    try {
                        Log.d("test","Entering query in initial avd when key not found key:"+ selection+" myport: "+myPort);
                        String msg = "query;0;" + selection + ";" + myPort + "\n";
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
                        String wait = blo.take();
                        Log.d("test", "String wait being populated: " + wait);
                        String[] strsplit2=wait.split(";");
                        cur.addRow(new Object[]{strsplit2[0],strsplit2[1]});
                        Log.d("test","Returning cursor object in loop"+cur);
                        return cur;
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Log.d("test", "Query function, Key: " + selection + " Value: " + input);
        }
        Log.d("test","Returning cursor object"+cur+" with size: "+cur.getCount());
        return cur;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    public class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while (true) {
                try {
                    Socket accept = serverSocket.accept();
                    ObjectInputStream instream = new ObjectInputStream(accept.getInputStream());

                    String recmsg = (String) instream.readObject();
                    ObjectOutputStream ostream=new ObjectOutputStream(accept.getOutputStream());
                    ostream.writeObject("Received "+recmsg);
                    ostream.flush();
                    ostream.close();
                    if (recmsg.contains("add")) {
                        String[] strsplit=recmsg.split(";");
                        Node nobj=new Node();
                        nobj.id=strsplit[1];
                        nobj.hid=strsplit[2];
                        nobj.succ=nobj.hid;
                        nobj.pred=nobj.hid;
                        lst.add(nobj);
                        Log.d("test","Server: Entering add, hid: "+nobj.hid);
                        Collections.sort(lst);
                        int sz=lst.size();
                        for (int i=0;i<sz;i++){
                            if(i==sz-1){
                                lst.get(i).succ=lst.get(0).hid;
                            }
                            else{
                                lst.get(i).succ=lst.get(i+1).hid;
                            }
                            if(i==0){
                                lst.get(i).pred=lst.get(sz-1).hid;
                            }
                            else{
                                lst.get(i).pred=lst.get(i-1).hid;
                            }
                        }
                        for(int i=0;i<lst.size();i++){
                            Log.d("test","ArrayList: "+i+" succ: "+lst.get(i).succ+" pred: "+lst.get(i).pred+" hid: "+lst.get(i).hid+" id: "+lst.get(i).id);
                        }
                    }
                    else if(recmsg.contains("insert")){
                        String[] strsplit=recmsg.split(";");
                        ContentValues cont=new ContentValues();
                        cont.put("key",strsplit[1]);
                        cont.put("value",strsplit[2]);
                        Log.d("test","Server: entering insert, key: "+strsplit[1]+" value: "+strsplit[2]);
                        insert(uriobject,cont);
                    }
                    else if(recmsg.contains("query;0")){
                        String[] strsplit=recmsg.split(";");
                        Log.d("test","Server: entering query;0, key: "+strsplit[2]+" myport in received string: "+strsplit[3]);
                        query(uriobject,null,strsplit[2]+";"+strsplit[3],null,null);
                    }
                    else if(recmsg.contains("query;1")){
                        String[] strsplit=recmsg.split(";");
                        Log.d("test","Server: entering query;1, key: "+strsplit[2]+" value: "+strsplit[3]);
                        try {
                            blo.put(strsplit[2]+";"+strsplit[3]);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(recmsg.contains("query;*")){
                        String[] strsplit=recmsg.split(";");
                        if(strsplit[2].compareTo("0")==0){
                            query(uriobject,null,strsplit[1]+";"+strsplit[2]+";"+strsplit[3],null,null);
                        }
                        else if(strsplit[2].compareTo("1")==0){
                            String pairs="*";
                            for(int i=4;i<strsplit.length;i+=2){
                                pairs+=";"+strsplit[i]+";"+strsplit[i+1];
                            }
                            Log.d("test","Server: Query(*) pairs: "+pairs);
                            try {
                                blo.put(pairs);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(recmsg.contains("delete")){
                        String[] strsplit=recmsg.split(";");
                        delete(uriobject,strsplit[1]+";"+strsplit[2],null);
                    }

//                    for(int i=0;i<lst.size();i++){
//                        Log.d("test","ArrayList: "+i+" succ: "+lst.get(i).succ+" pred: "+lst.get(i).pred+" hid: "+lst.get(i).hid+" id: "+lst.get(i).id);
//                    }

                    //publishProgress(recmsg);
                } catch (OptionalDataException e) {
                    e.printStackTrace();
                } catch (StreamCorruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
        }
    }

    public class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String msgToSend = msgs[0].trim();
            String response = null;
            if (msgToSend.startsWith("add")) {
                for (int i = 0; i < 5; i++) {
                    try {
                        if(REMOTE_PORT[i].compareTo(msgs[1])==0){
                            continue;
                        }
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT[i]));

                        /*
                         * TODO: Fill in your client code that sends out a message.
                         */
                        ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                        Log.d("test", "ClientTask before sending(if): " + msgToSend);
                        ostream.writeObject(msgToSend);

                        ObjectInputStream instream = new ObjectInputStream(socket.getInputStream());
                        response = String.valueOf(instream.readObject());
                        ostream.flush();
                        instream.close();
                        ostream.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if(msgToSend.startsWith("insert")) {
                try {
                    Log.d("test","Sending message forward, message: "+msgToSend);
                    String suc=null;
                    String port=null;
                    Iterator iter=lst.iterator();
                    while(iter.hasNext()){
                        Node nobj= (Node) iter.next();
                        if(nobj.hid.compareTo(gportStr)==0){
                            suc=nobj.succ;
                            Log.d("test","Succ hid: "+suc+" my hid: "+nobj.hid+" my id: "+nobj.id);
                            break;
                        }
                    }
                    Iterator iter2=lst.iterator();
                    while(iter2.hasNext()){
                        Node nobj2= (Node) iter2.next();
                        if(nobj2.hid.compareTo(suc)==0){
                            port=nobj2.id;
                            Log.d("test","Succ port: "+port);
                            break;
                        }
                    }
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                    Log.d("test", "ClientTask before sending(else - insert): " + msgToSend);
                    ostream.writeObject(msgToSend);

                    ObjectInputStream instream = new ObjectInputStream(socket.getInputStream());
                    response = String.valueOf(instream.readObject());
                    instream.close();
                    ostream.flush();
                    ostream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else if(msgToSend.startsWith("query;0")){
                try {
                    Log.d("test","Sending message forward, message: "+msgToSend);
                    String suc=null;
                    String port=null;
                    Iterator iter=lst.iterator();
                    while(iter.hasNext()){
                        Node nobj= (Node) iter.next();
                        if(nobj.hid.compareTo(gportStr)==0){
                            suc=nobj.succ;
                            Log.d("test","Succ hid: "+suc+" my hid: "+nobj.hid+" my id: "+nobj.id);
                            break;
                        }
                    }
                    Iterator iter2=lst.iterator();
                    while(iter2.hasNext()){
                        Node nobj2= (Node) iter2.next();
                        if(nobj2.hid.compareTo(suc)==0){
                            port=nobj2.id;
                            Log.d("test","Succ port: "+port);
                            break;
                        }
                    }
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                    Log.d("test", "ClientTask before sending(else - query;0): " + msgToSend);
                    ostream.writeObject(msgToSend);

                    ObjectInputStream instream = new ObjectInputStream(socket.getInputStream());
                    response = String.valueOf(instream.readObject());
                    instream.close();
                    ostream.flush();
                    ostream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else if(msgToSend.startsWith("query;1")){
                try {
                    Log.d("test","Sending message forward, message: "+msgToSend);
                    String[] strsplit=msgToSend.split(";");
                    String port=strsplit[4];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                    Log.d("test", "ClientTask before sending(else - query;1): " + msgToSend);
                    ostream.writeObject(msgToSend);

                    ObjectInputStream instream = new ObjectInputStream(socket.getInputStream());
                    response = String.valueOf(instream.readObject());
                    instream.close();
                    ostream.flush();
                    ostream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else if (msgToSend.startsWith("query;*")) {
                String port=msgs[1];
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    /*
                    * TODO: Fill in your client code that sends out a message.
                    */
                    ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                    Log.d("test", "ClientTask before sending(else - query;*): " + msgToSend);
                    ostream.writeObject(msgToSend);

                    ObjectInputStream instream = new ObjectInputStream(socket.getInputStream());
                    response = String.valueOf(instream.readObject());
                    ostream.flush();
                    instream.close();
                    ostream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else if(msgToSend.startsWith("delete")){
                String port=msgs[1];
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    /*
                     * TODO: Fill in your client code that sends out a message.
                     */
                    ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                    Log.d("test", "ClientTask before sending(else - query;*): " + msgToSend);
                    ostream.writeObject(msgToSend);

                    ObjectInputStream instream = new ObjectInputStream(socket.getInputStream());
                    response = String.valueOf(instream.readObject());
                    ostream.flush();
                    instream.close();
                    ostream.close();
                    socket.close();
                    blo.put("null");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    class Node implements Comparable<Node> {
        String succ;
        String pred;
        String id;
        String hid;

        @Override
        public int compareTo(Node o) {
            if (o.hid.compareTo(this.hid)>0){
                return -1;
            }
            else if (o.hid.compareTo(this.hid)<0){
                return 1;
            }
            return 0;
        }
    }
}