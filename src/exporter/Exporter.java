package exporter;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ua2.edf.EDFData;
import org.ua2.edf.parser.EDFParser;
import org.ua2.edf.parser.ParseException;
import org.ua2.edf.parser.TokenMgrError;

public class Exporter {
	
	private static class User {
		String name;
		Map<Long, String> oldNames = null;
	}
	
	public static void close(Connection conn, Statement stmt, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
		if (stmt != null) {
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
		if (conn != null) {
			try {
				stmt.close();
			} catch (Exception e) {
			}
		}
	}
	
	private static EDFData getData(String edf) {
		if(edf == null || edf.length() == 0) {
			return null;
		}
		
		//System.out.println("Parsing EDF");
        Reader reader = new StringReader(edf);
        EDFParser parser = new EDFParser(reader);
        try {
        	EDFData data = parser.elementtree();
            return data;
        } catch(TokenMgrError e) {
        	System.err.println("Bad token in " + edf + ", " + e);
        	return null;
        } catch(ParseException e) {
        	System.err.println("Cannot parse " + edf + ", " + e);
        	return null;
        }
	}
	
	public static Map<Integer, String> getFolderLookup(Connection conn) throws SQLException {
		Map<Integer, String> map = new HashMap<Integer, String>(); 
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select folderid,name from folder_item where mod(accessmode,2)=1");
			while(rs.next()) {
				map.put(rs.getInt("folderid"), rs.getString("name"));
			}
		} finally {
			close(null, stmt, rs);
		}
		return map;
	}
	
	private static String getName(int id, long messageDate, Map<Integer, User> userLookup) {
		User user = userLookup.get(id);
		if(user == null) {
			return null;
		}
		
		if(user.oldNames == null) {
			return user.name;
		}
		
		String name = user.name;
		long latest = 0;
		for(Entry<Long, String> entry : user.oldNames.entrySet()) {
			if(entry.getKey() > latest && entry.getKey() > messageDate) {
				//System.out.println("Setting name to " + entry.getValue() + " for " + entry.getKey());
				name = entry.getValue();
				latest = entry.getKey();
			}
		}
		
		return name;
	}

	public static Map<Integer, User> getUserLookup(Connection conn) throws SQLException, ParseException {
		Map<Integer, User> map = new HashMap<Integer, User>(); 
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select userid,name,edf from user_item");
			while(rs.next()) {
				User user = new User();
				map.put(rs.getInt("userid"), user);
				
				user.name = rs.getString("name");
				
				String edf = rs.getString("edf");
				EDFData data = getData(edf);
				if(data != null) {
			        List<EDFData> oldNames = data.getChildren("oldname");
			        if(oldNames != null && oldNames.size() > 0) {
			        	user.oldNames = new HashMap<Long, String>();
				        for(EDFData oldName : oldNames) {
				        	long key = oldName.getInteger();
				        	String name = oldName.getChild("name").getString();
				        	//System.out.println("Old name " + key + " -> " + name);
				        	user.oldNames.put(key, name);
				        }
			        }
				}
			}
		} finally {
			close(null, stmt, rs);
		}
		
		return map;
	}
	
	private static void printHSET(int id, String name, String value) {
		System.out.println("hset message:" + id + " " + name + " \"" + value + "\"");
	}
	
	private static int export(Connection conn, long minDate, Map<Integer, String> folderLookup, Map<Integer, User> userLookup) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		int messageCount = 0;
		try {
			stmt = conn.createStatement();
			
			String query = "select messageid,parentid,folderid,message_date,fromid,toid,message_text,subject,edf from folder_message_item ";
			if(minDate != -1) {
				query += "where message_date>=" + minDate;
			}
			query += " order by messageid";
			
			rs = stmt.executeQuery(query);
			while(rs.next()) {
				try {
					int folderId = rs.getInt("folderid");
					String folder = folderLookup.get(folderId);
					
					if(folder != null) {
						int id = rs.getInt("messageid");
						int parentId = rs.getInt("parentid");
						String subject = rs.getString("subject");
						long epoch = rs.getLong("message_date");
						String body = rs.getString("message_text");
						EDFData data = getData(rs.getString("edf"));
	
						String from = getName(rs.getInt("fromid"), epoch, userLookup);
						String to = getName(rs.getInt("toid"), epoch, userLookup);
						if(to == null && data != null && data.getChild("toname") != null) {
							to = data.getChild("toname").getString();
						}
						
						if(body != null) {
							body = body.replace("\"", "\\\"");
							body = body.replace("\r", "\\r");
							body = body.replace("\n", "\\n");
						}
						
						System.out.println("\n# Message ID = " + id);
						printHSET(id, "id", Integer.toString(id));
						printHSET(id, "folder", folderLookup.get(folderId));
						printHSET(id, "from", from);
						if(to != null) {
							printHSET(id, "to", to);
						}
						printHSET(id, "subject", subject);
						printHSET(id, "parent", Integer.toString(parentId));
						printHSET(id, "epoch", Long.toString(epoch));
						System.out.println("set body:" + id + " \"" + body + "\"");
						
						messageCount++;
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			close(null, stmt, rs);
		}
		
		System.out.println("# Exported " + messageCount + " mesages");
		
		return messageCount;
	}

	public static void main(String[] args) {
		Connection conn = null;
		
		if(args.length != 3) {
			System.err.println("Usage: Exporter <database> <username> <password> [<mindate>]");
			System.exit(1);
		}

		try {
			String db = args[0];
			String user = args[1];
			String password = args[2];
			long minDate = -1;
			if(args.length == 4) {
				minDate = Long.parseLong(args[3]);
			}
			
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost/" + db + "?user=" + user + "&password=" + password);

			Map<Integer, User> userLookup = getUserLookup(conn);
			
			Map<Integer, String> folderLookup = getFolderLookup(conn);

			export(conn, minDate, folderLookup, userLookup);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(conn, null, null);
		}
		
		System.exit(0);
	}	
}
