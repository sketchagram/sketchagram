package sketchagram.chalmers.com.sketchagram;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

import sketchagram.chalmers.com.database.SketchagramDb;
import sketchagram.chalmers.com.model.ClientMessage;
import sketchagram.chalmers.com.model.Contact;
import sketchagram.chalmers.com.model.Conversation;
import sketchagram.chalmers.com.model.MessageType;
import sketchagram.chalmers.com.model.Profile;
import sketchagram.chalmers.com.model.User;
import sketchagram.chalmers.com.model.UserManager;

/**
 * Created by Olliver on 15-04-21.
 */
public class DatabaseTest extends BasicSetupTest {
    SketchagramDb db;

    protected void setUp() throws Exception {
        super.setUp();
        db = new SketchagramDb(getContext());
        db.update();
    }

    public void testInsertMessage(){
        List<Contact> contactList = new ArrayList<>();
        User user1 = new User("user1", new Profile());
        UserManager.getInstance().login(TEST_USERNAME, TEST_PASSWORD);
        Contact user2 = new Contact("user2", new Profile());
        contactList.add(user2);
        ClientMessage message = new ClientMessage(System.currentTimeMillis(), user1, contactList, "hej", MessageType.TEXTMESSAGE);
        db.insertMessage(message);
        List<Conversation> conversationList = db.getAllConversations(user1.getUsername());
        boolean exist = false;
        for(Conversation conversation : conversationList){
            exist = conversation.getHistory().contains(message);
            if(exist)
                break;
        }
        assertTrue(exist);
    }
}
