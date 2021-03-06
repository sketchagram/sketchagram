package sketchagram.chalmers.com.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.view.MotionEvent;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sketchagram.chalmers.com.model.ADigitalPerson;
import sketchagram.chalmers.com.model.ClientMessage;
import sketchagram.chalmers.com.model.Contact;
import sketchagram.chalmers.com.database.DataContract.*;
import sketchagram.chalmers.com.model.Conversation;
import sketchagram.chalmers.com.model.Drawing;
import sketchagram.chalmers.com.model.DrawingEvent;
import sketchagram.chalmers.com.model.Emoticon;
import sketchagram.chalmers.com.model.MessageType;
import sketchagram.chalmers.com.model.Profile;
import sketchagram.chalmers.com.model.User;

/**
 * Created by Alex and Olliver on 2015-03-06.
 */
public class SketchagramDb {

    private static final String WHERE = " WHERE ";
    private static final String FROM = " FROM ";
    private static final String SELECT_ALL = "SELECT * ";
    private static final String EQUALS = " = ";
    private static final String AND = " AND ";
    private static final String QUESTION_MARK = " ? ";
    private static final String OR = " OR ";
    private static final String SELECT = "SELECT ";
    private static final String IN = " IN ";
    private SQLiteDatabase db;
    private DBHelper dbh;

    public SketchagramDb (Context context) {
        dbh = new DBHelper(context);

        try {
            db = dbh.getWritableDatabase();
        }catch (SQLiteException e){
            System.out.println(e.getMessage());

        }
    }

    public void update() {
        dbh.onUpgrade(db);
    }
    public boolean insertContact  (Contact contact) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactTable.COLUMN_NAME_CONTACT_USERNAME, contact.getUsername().toLowerCase());
        contentValues.put(ContactTable.COLUMN_NAME_CONTACT_NAME, contact.getProfile().getName());
        contentValues.put(ContactTable.COLUMN_NAME_CONTACT_EMAIL, contact.getProfile().getEmail());
        contentValues.put(ContactTable.COLUMN_NAME_LAST_ACCESSED, contact.getLastAccessed());
        long i = db.insert(ContactTable.TABLE_NAME, null, contentValues);
        return true;
    }

    /**
     * Removes a contact from the database.
     * @param contact the one to be removed
     * @return true if successfully removed, false otherwise.
     */
    public boolean deleteContact (Contact contact) {
        return db.delete(ContactTable.TABLE_NAME,
                ContactTable.COLUMN_NAME_CONTACT_USERNAME + EQUALS + QUESTION_MARK,
                new String[] { contact.getUsername().toLowerCase() }) > 0;
    }

    public void updateContact (Contact contact ) {
        deleteContact(contact);
        insertContact(contact);
    }

    public ArrayList<Contact> getAllContacts() {
        ArrayList<Contact> contacts = new ArrayList();
        Cursor res =  db.rawQuery( SELECT_ALL + FROM + ContactTable.TABLE_NAME, null );
        res.moveToFirst();
        while(res.isAfterLast() == false){
            String name = res.getString(res.getColumnIndexOrThrow(ContactTable.COLUMN_NAME_CONTACT_NAME));
            String email = res.getString(res.getColumnIndexOrThrow(ContactTable.COLUMN_NAME_CONTACT_EMAIL));
            String id = res.getString(res.getColumnIndexOrThrow(ContactTable.COLUMN_NAME_CONTACT_USERNAME));
            int lastAccessed = res.getInt(res.getColumnIndexOrThrow(ContactTable.COLUMN_NAME_LAST_ACCESSED));
            Profile profile = new Profile();
            profile.setNickName(email);
            profile.setName(name);
            Contact c = new Contact(id, profile, lastAccessed);
            contacts.add(c);
            res.moveToNext();
        }
        return contacts;
    }

    private Contact getContact(String userName){
        Cursor res =  db.rawQuery( SELECT_ALL + FROM + ContactTable.TABLE_NAME +
                WHERE + ContactTable.COLUMN_NAME_CONTACT_USERNAME + EQUALS + QUESTION_MARK
                , new String[]{userName.toLowerCase()} );
        res.moveToFirst();
        if(!res.isAfterLast()) {
            String name = res.getString(res.getColumnIndexOrThrow(ContactTable.COLUMN_NAME_CONTACT_NAME));
            String email = res.getString(res.getColumnIndexOrThrow(ContactTable.COLUMN_NAME_CONTACT_EMAIL));
            String id = res.getString(res.getColumnIndexOrThrow(ContactTable.COLUMN_NAME_CONTACT_USERNAME));
            Profile profile = new Profile();
            profile.setNickName(email);
            profile.setName(name);
            return new Contact(id, profile);
        }
        return null;

    }

    public int insertMessage (ClientMessage message) {
        ContentValues contentValues = new ContentValues();
        List<ADigitalPerson> participants = new ArrayList<>();
        participants.addAll(message.getReceivers());
        boolean exists = false;
        for(ADigitalPerson participant : participants){
            if(participant.getUsername().toLowerCase().equals(message.getSender().getUsername().toLowerCase())){
                exists = true;
            }
        }
        if(!exists) {
            participants.add(message.getSender());
        }
        int i = insertConversation(participants);
        if(i < 0){
            return i;
        }
        contentValues.put(MessagesTable.COLUMN_NAME_SENDER, message.getSender().getUsername().toLowerCase());
        contentValues.put(MessagesTable.COLUMN_NAME_CONVERSATION_ID, i);
        contentValues.put(MessagesTable.COLUMN_NAME_TIMESTAMP, message.getTimestamp());
        contentValues.put(MessagesTable.COLUMN_NAME_TYPE, message.getType().toString());
        contentValues.put(MessagesTable.COLUMN_NAME_CONTENT, new Gson().toJson(message.getContent()));
        contentValues.put(MessagesTable.COLUMN_NAME_READ, message.isRead() ? 1:0);
        db.insert(MessagesTable.TABLE_NAME, null, contentValues);
        return i;
    }

    /**
     *
     * @deprecated not in use
     */
    public Integer deleteMessage (ClientMessage message) {
        return db.delete(MessagesTable.TABLE_NAME,
                (MessagesTable.COLUMN_NAME_TIMESTAMP + EQUALS + QUESTION_MARK + AND + MessagesTable.COLUMN_NAME_SENDER + EQUALS + QUESTION_MARK ),
                new String[] { String.valueOf(message.getTimestamp()), message.getSender().getUsername().toLowerCase() });
    }

    private int insertConversation(List<ADigitalPerson> participants){
        int i = maxValue(ConversationTable.TABLE_NAME, ConversationTable.COLUMN_NAME_CONVERSATION_ID);
        int exists = conversationExists(participants);
        if( exists < 0 ) {
            for (ADigitalPerson person : participants) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(ConversationTable.COLUMN_NAME_CONVERSATION_ID, i);
                contentValues.put(ConversationTable.COLUMN_NAME_PARTICIPANT, person.getUsername().toLowerCase());
                db.insert(ConversationTable.TABLE_NAME, null, contentValues);
            }
            return i;
        }
        return exists;


    }

    public void removeConversation(Conversation conversation){
        db.delete(MessagesTable.TABLE_NAME,
                (MessagesTable.COLUMN_NAME_CONVERSATION_ID + EQUALS + QUESTION_MARK),
                new String[] { String.valueOf(conversation.getConversationId())});
        db.delete(ConversationTable.TABLE_NAME,
                (ConversationTable.COLUMN_NAME_CONVERSATION_ID + EQUALS + QUESTION_MARK),
                new String[] { String.valueOf(conversation.getConversationId())});
    }

    private int conversationExists(List<ADigitalPerson> participants){
        List<Conversation> convList = getAllConversations();
        for(Conversation c : convList){
            boolean same = true;
            if(c.getParticipants().size() != participants.size()){
                same = false;
            }
            for(ADigitalPerson participant : c.getParticipants()) {
                if (!same){
                    break;
                }
                boolean participantexists = false;
                for(ADigitalPerson receiver : participants){
                    if(participant.equals(receiver)){
                        participantexists = true;
                        break;
                    }
                }
                if(!participantexists){
                    same = false;
                    break;
                }
            }
            if(same){
                return c.getConversationId();
            }
        }
        return -1;
    }

    /**
     * @deprecated not in use
     * @param participants
     * @return
     */
    private int getConversation(List<ADigitalPerson> participants) {
        String query = SELECT + ConversationTable.COLUMN_NAME_CONVERSATION_ID + FROM + ConversationTable.TABLE_NAME +
                WHERE + ConversationTable.COLUMN_NAME_CONVERSATION_ID + IN + " (";
        boolean first = true;
        String[] queryParticipants = new String[participants.size()];
        int i = 0;
        for(ADigitalPerson participant : participants) {
            if (first) {
                query += SELECT + ConversationTable.COLUMN_NAME_CONVERSATION_ID + FROM + ConversationTable.TABLE_NAME +
                        WHERE + ConversationTable.COLUMN_NAME_PARTICIPANT + EQUALS + QUESTION_MARK + " ) ";
                first = false;
            }else {
                query += AND + " (" + SELECT + ConversationTable.COLUMN_NAME_CONVERSATION_ID + FROM + ConversationTable.TABLE_NAME +
                        WHERE + ConversationTable.COLUMN_NAME_PARTICIPANT + EQUALS + QUESTION_MARK + " ) ";
            }
            queryParticipants[i] = participant.getUsername().toLowerCase();
            i++;

        }
        Cursor cur = db.rawQuery(query,  queryParticipants);
        cur.moveToFirst();
        return cur.getInt(cur.getColumnIndexOrThrow(ConversationTable.COLUMN_NAME_CONVERSATION_ID));

    }


    private List<Conversation> getAllConversations(){
        ArrayList<Conversation> conversations = new ArrayList();
        Cursor res =  db.rawQuery( SELECT_ALL + FROM + ConversationTable.TABLE_NAME, null);
        int current = -1;
        res.moveToFirst();
        List<ADigitalPerson> participants = new ArrayList<>();
        while(res.isAfterLast() == false){
            int temp = res.getInt(res.getColumnIndexOrThrow(ConversationTable.COLUMN_NAME_CONVERSATION_ID));
            if(temp != current) {
                current = temp;
            }
            String contactUsername = res.getString(res.getColumnIndexOrThrow(ConversationTable.COLUMN_NAME_PARTICIPANT));
            participants.add(new Contact(contactUsername, new Profile()));
            res.moveToNext();
            if(!res.isAfterLast()) {
                temp = res.getInt(res.getColumnIndexOrThrow(ConversationTable.COLUMN_NAME_CONVERSATION_ID));
            }
            if(temp > current || res.isAfterLast()){
                Cursor cursor = db.rawQuery(SELECT_ALL + FROM + MessagesTable.TABLE_NAME +
                                WHERE + MessagesTable.COLUMN_NAME_CONVERSATION_ID + EQUALS + QUESTION_MARK,
                        new String[]{String.valueOf(current)});
                cursor.moveToFirst();
                List<ClientMessage> messages = new ArrayList<>();
                while (!cursor.isAfterLast()) {
                    String type = cursor.getString(cursor.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_TYPE));
                    String content = cursor.getString(cursor.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_CONTENT));
                    long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_TIMESTAMP));
                    String sender = cursor.getString(cursor.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_SENDER));
                    boolean read = cursor.getInt(cursor.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_READ)) == 0 ? false : true;
                    Gson gson = new Gson();
                    MessageType typeEnum = MessageType.valueOf(type);
                    ADigitalPerson contactSender = new Contact(sender, new Profile());
                    participants.remove(contactSender);

                    switch(typeEnum) {
                        case TEXTMESSAGE:
                            String decodedContent = gson.fromJson(content, String.class);
                            messages.add(new ClientMessage(timestamp, contactSender, participants, decodedContent, typeEnum, read));
                            break;
                        case EMOTICON:
                            Emoticon decodedEmoticon = gson.fromJson(content, Emoticon.class);
                            messages.add(new ClientMessage(timestamp, contactSender, participants, decodedEmoticon, typeEnum, read));
                            break;
                        case DRAWING:
                            Drawing decodedDrawing = gson.fromJson(content, Drawing.class);
                            messages.add(new ClientMessage(timestamp, contactSender, participants, decodedDrawing, typeEnum, read));
                            break;

                    }
                    participants.add(contactSender);
                    cursor.moveToNext();
                }
                conversations.add(new Conversation(participants, messages, current));
                participants = new ArrayList<>();
            }

        }
        return conversations;
    }

    public List<Conversation> getAllConversations(String user) {
        List<Conversation> userConversations = new ArrayList<>();
        for(Conversation conversation : getAllConversations()){
            for(ADigitalPerson participant : conversation.getParticipants()){
                if(participant.getUsername().toLowerCase().equals(user.toLowerCase())){
                    userConversations.add(conversation);
                }
            }
        }
        Collections.sort(userConversations);
        return userConversations;
    }



    private int maxValue(String table, String column) {
        Cursor cur = db.rawQuery("SELECT MAX(" + column + ") AS "+ column + FROM + table, null);
        cur.moveToFirst();
        int max = cur.getInt(cur.getColumnIndexOrThrow(column)) + 1;
        return max;
    }


    /**
     * @deprecated
     * @param contacts
     * @return
     */
    public ArrayList<ClientMessage> getAllMessagesFromContacts(List<Contact> contacts) {
        throw new UnsupportedOperationException();
        //TODO: finish this method
        /*ArrayList<ClientMessage> messages = new ArrayList();
        Cursor res =  db.rawQuery( SELECT_ALL + FROM + MessagesTable.TABLE_NAME , null );
        res.moveToFirst();
        while(res.isAfterLast() == false){
            int messsageId = res.getInt(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_MESSAGE_ID));
            String contact = res.getString(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_CONTACT_USERNAME));

            messages.add(null);
            res.moveToNext();
        }
        return messages;*/
    }
/*
    public ArrayList<ClientMessage> getAllMessages() {
        ArrayList<ClientMessage> messages = new ArrayList();
        Cursor res =  db.rawQuery( SELECT_ALL + FROM + MessagesTable.TABLE_NAME, null);
        res.moveToFirst();
        while(res.isAfterLast() == false){
            String content = res.getString(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_CONTENT));
            String type = res.getString(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_TYPE));
            long timestamp = res.getLong(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_TIMESTAMP));
            String sender = res.getString(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_SENDER));
            Gson gson = new Gson();
            MessageType typeEnum = MessageType.valueOf(type);
            switch(typeEnum) {
                case TEXTMESSAGE:
                    String decodedContent = gson.fromJson(content, String.class);
                    List<ADigitalPerson> receivers = new ArrayList<>();
                    receivers.add(new Contact(receiver, new Profile()));
                    messages.add(new ClientMessage(timestamp, new Contact(sender, new Profile()), receivers, decodedContent, typeEnum));
                    break;
                case EMOTICON:
                    //TODO: decode here
                    break;
                case DRAWING:
                    Drawing decodedDrawing = gson.fromJson(content, Drawing.class);
                    List<ADigitalPerson> drawingReceivers = new ArrayList<>();
                    drawingReceivers.add(new Contact(receiver, new Profile()));
                    messages.add(new ClientMessage(timestamp, new Contact(sender, new Profile()), drawingReceivers, decodedDrawing, typeEnum));
                    break;

            }
            res.moveToNext();
        }
        return messages;
    }

    public ArrayList<ClientMessage> getAllMessagesFromAContact(ADigitalPerson contact) {
        ArrayList<ClientMessage> messages = new ArrayList();
        Cursor res =  db.rawQuery( SELECT_ALL + FROM + MessagesTable.TABLE_NAME + WHERE + MessagesTable.COLUMN_NAME_SENDER
                + EQUALS + QUESTION_MARK, new String[] {contact.getUsername(), contact.getUsername()} );
        res.moveToFirst();
        while(res.isAfterLast() == false){
            String content = res.getString(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_CONTENT));
            String type = res.getString(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_TYPE));
            long timestamp = res.getLong(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_TIMESTAMP));
            String sender = res.getString(res.getColumnIndexOrThrow(MessagesTable.COLUMN_NAME_SENDER));
            Gson gson = new Gson();
            MessageType typeEnum = MessageType.valueOf(type);
            switch(typeEnum) {
                case TEXTMESSAGE:
                    String decodedContent = gson.fromJson(content, String.class);
                    List<ADigitalPerson> receivers = new ArrayList<>();
                    receivers.add(new Contact(receiver, new Profile()));
                    messages.add(new ClientMessage(timestamp, new Contact(sender, new Profile()), receivers, decodedContent, typeEnum));
                    break;
                case EMOTICON:
                    //TODO: decode here
                    break;
                case DRAWING:
                    Drawing decodedDrawing = gson.fromJson(content, Drawing.class);
                    List<ADigitalPerson> drawingReceivers = new ArrayList<>();
                    drawingReceivers.add(new Contact(receiver, new Profile()));
                    messages.add(new ClientMessage(timestamp, new Contact(sender, new Profile()), drawingReceivers, decodedDrawing, typeEnum));
                    break;

            }
            res.moveToNext();
        }
        return messages;
    }
*/

    /**
     * @deprecated
     * @param tableName
     * @return
     */
    private int numberOfRows(String tableName){
        int numRows = (int) DatabaseUtils.queryNumEntries(db, tableName);
        return numRows;
    }

}

