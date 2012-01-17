package org.ua2.apuawling.edf;

import junit.framework.TestCase;

import org.ua2.apuawling.Action;

public class TestEDFActions extends TestCase {
	public void testFolders() {
		Action<?> action = new GetFoldersAction(null);

		assertTrue(action.isMatch("GET", "/folders"));
		assertTrue(action.isMatch("GET", "/folders/all"));
		assertTrue(action.isMatch("GET", "/folders/subscribed"));
		assertTrue(action.isMatch("GET", "/folders/unread"));

		assertFalse(action.isMatch("GET", "/folder/Applications"));
		assertFalse(action.isMatch("GET", "/folder/Applications/full"));
		assertFalse(action.isMatch("GET", "/folder/Applications/unread/full"));

		assertFalse(action.isMatch("GET", "/folders/foo"));
		assertFalse(action.isMatch("GET", "/folders/bar"));
	}

	public void testSubscribe() {
		Action<?> action = new SubscribeAction(null);

		assertTrue(action.isMatch("POST", "/folder/Applications/subscribe"));
		assertTrue(action.isMatch("POST", "/folder/Applications/unsubscribe"));
		
		assertFalse(action.isMatch("POST", "/folder/subscribe"));
		assertFalse(action.isMatch("POST", "/folder/Applications/unsubscribe/defintely"));
	}

	public void testFolder() {
		Action<?> action = new GetFolderAction(null);

		assertTrue(action.isMatch("GET", "/folder/Applications"));
		assertTrue(action.isMatch("GET", "/folder/Applications/full"));
		assertTrue(action.isMatch("GET", "/folder/Applications/unread/full"));
		assertTrue(action.isMatch("GET", "/folder/For-Sale"));
		assertTrue(action.isMatch("GET", "/folder/Free-Venting"));
		assertTrue(action.isMatch("GET", "/folder/Long-Posts"));
		assertTrue(action.isMatch("GET", "/folder/Long-Posts.D"));
		assertTrue(action.isMatch("GET", "/folder/Long-Posts.D/unread/full"));
		assertTrue(action.isMatch("GET", "/folder/Long-Posts/unread/full"));
		assertTrue(action.isMatch("GET", "/folder/null/full"));
		assertTrue(action.isMatch("GET", "/folder/private"));
		assertTrue(action.isMatch("GET", "/folder/Private"));
		assertTrue(action.isMatch("GET", "/folder/Web-Dev/unread/full"));
		assertTrue(action.isMatch("GET", "/folder/Who-Lists-How-Boring"));
		assertTrue(action.isMatch("GET", "/folder/Who-Lists-How-Boring/unread/full"));
		assertTrue(action.isMatch("GET", "/folder/Windows"));
		assertTrue(action.isMatch("GET", "/folder/Windows/unread/full"));
		assertTrue(action.isMatch("GET", "/folder/Words"));
		assertTrue(action.isMatch("GET", "/folder/Words/unread/full"));
		assertTrue(action.isMatch("GET", "/folder/Work"));
		assertTrue(action.isMatch("GET", "/folder/Work/unread/full"));
		assertTrue(action.isMatch("GET", "/folder/WoW/unread/full"));

		assertFalse(action.isMatch("GET", "/folders"));
		assertFalse(action.isMatch("GET", "/folders/all"));

		assertFalse(action.isMatch("GET", "/folder/things/stuff"));
		assertFalse(action.isMatch("GET", "/fold"));
	}

	public void testMessages() {
		Action<?> action = new GetMessagesAction(null);

		assertTrue(action.isMatch("GET", "/messages/saved"));
	}
	
	public void testMessage() {
		Action<?> action = new GetMessageAction(null);

		assertTrue(action.isMatch("GET", "/message/123"));

		assertFalse(action.isMatch("GET", "/message"));
		assertFalse(action.isMatch("GET", "/msg/123"));
		assertFalse(action.isMatch("GET", "/message/123/what"));
	}

	public void testMarkMessage() {
		Action<?> action = new MarkMessageAction(null);

		assertTrue(action.isMatch("POST", "/message/read"));
		assertTrue(action.isMatch("POST", "/message/unread"));

		assertFalse(action.isMatch("POST", "/message/mark"));
	}

	public void testCatchupMessage() {
		Action<?> action = new CatchupAction(null);

		assertTrue(action.isMatch("POST", "/catchup/message"));
		assertTrue(action.isMatch("POST", "/catchup/folder/sticky"));

		assertFalse(action.isMatch("POST", "/catch"));
	}

	public void testCreateThread() {
		Action<?> action = new CreateThreadAction(null);

		assertTrue(action.isMatch("POST", "/folder/Food"));
		assertTrue(action.isMatch("POST", "/folder/footy"));
		assertTrue(action.isMatch("POST", "/folder/Here-And-Now"));
		assertTrue(action.isMatch("POST", "/folder/Manc-Chat"));
		assertTrue(action.isMatch("POST", "/folder/media"));
		assertTrue(action.isMatch("POST", "/folder/net"));
		assertTrue(action.isMatch("POST", "/folder/private"));
		assertTrue(action.isMatch("POST", "/folder/programming"));
		assertTrue(action.isMatch("POST", "/folder/Telecoms"));
		assertTrue(action.isMatch("POST", "/folder/test1"));
		assertTrue(action.isMatch("POST", "/folder/Transport"));
		assertTrue(action.isMatch("POST", "/folder/TwentySomething"));
		assertTrue(action.isMatch("POST", "/folder/UA"));

		assertFalse(action.isMatch("POST", "/folders"));
		assertFalse(action.isMatch("POST", "/folder/other/thing"));
	}

	public void testReplyMessage() {
		Action<?> action = new ReplyMessageAction(null);

		assertTrue(action.isMatch("POST", "/message/123"));

		assertFalse(action.isMatch("POST", "/messages/123"));
		assertFalse(action.isMatch("POST", "/message/123/4"));
	}

	public void testUser() {
		Action<?> action = new GetUserAction(null);

		assertTrue(action.isMatch("GET", "/user"));

		assertFalse(action.isMatch("GET", "/users"));
	}

	public void testUsers() {
		Action<?> action = new GetUsersAction(null);

		assertTrue(action.isMatch("GET", "/users"));
		assertTrue(action.isMatch("GET", "/users/online"));

		assertFalse(action.isMatch("GET", "/user"));
	}

	public void testSystem() {
		Action<?> action = new SystemAction(null);

		assertTrue(action.isMatch("GET", "/system"));
	}

	/*
	public void testBrowse() {
		Action<?> action = new GetSystemAction(null);

		assertTrue(action.isMatch("GET", "/browse/folder/moan"));
		assertTrue(action.isMatch("GET", "/browse/folders/unread"));
		assertTrue(action.isMatch("GET", "/browse/message/123"));
		assertTrue(action.isMatch("GET", "/browse/system"));
		
	}
	*/
	
	public void testLookup() {
		EDFProvider provider = new EDFProvider(null, 0, null, null, null, null);

		assertTrue(provider.getAction("GET", "/folders") instanceof GetFoldersAction);

		assertTrue(provider.getAction("POST", "/folder/Abuse/subscribe") instanceof SubscribeAction);

		assertTrue(provider.getAction("GET", "/folder/Abuse") instanceof GetFolderAction);
		
		assertTrue(provider.getAction("GET", "/message/123") instanceof GetMessageAction);
		assertTrue(provider.getAction("POST", "/message/read") instanceof MarkMessageAction);
		assertTrue(provider.getAction("POST", "/catchup/message/sticky") instanceof CatchupAction);

		assertTrue(provider.getAction("POST", "/folder/UA") instanceof CreateThreadAction);
		assertTrue(provider.getAction("POST", "/message/123") instanceof ReplyMessageAction);

		assertTrue(provider.getAction("GET", "/user") instanceof GetUserAction);

		assertTrue(provider.getAction("GET", "/users") instanceof GetUsersAction);
		assertTrue(provider.getAction("GET", "/users/online") instanceof GetUsersAction);

		assertTrue(provider.getAction("GET", "/system") instanceof SystemAction);
		
		assertNull(provider.getAction("GET", "/thing"));
	}
}
