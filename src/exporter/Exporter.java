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

import org.ua2.edf.EDFData;
import org.ua2.edf.parser.EDFParser;
import org.ua2.edf.parser.ParseException;

public class Exporter {
	
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
	
	public static Map<Integer, Map<Integer, String>> getUserLookup(Connection conn) throws SQLException, ParseException {
		Map<Integer, Map<Integer, String>> map = new HashMap<Integer, Map<Integer, String>>(); 
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select userid,name,edf from user_item");
			while(rs.next()) {
				Map<Integer, String> lookup = new HashMap<Integer, String>();
				map.put(rs.getInt("userid"), lookup);
				
				lookup.put(0, rs.getString("name"));
				
				String edf = rs.getString("edf");
				if(edf != null && edf.length() > 0) {
					System.out.println("Parsing:\n" + edf);
					if(edf.indexOf("content-type") < 0 && edf.indexOf("<=") < 0) {
				        Reader reader = new StringReader(edf);
				        EDFParser parser = new EDFParser(reader);
				        EDFData data = parser.elementtree();
				        List<EDFData> oldNames = data.getChildren("oldname");
				        for(EDFData oldName : oldNames) {
				        	lookup.put(oldName.getInteger(), oldName.getChild("name").getString());
				        }
					}
				}
			}
		} finally {
			close(null, stmt, rs);
		}
		return map;
	}
	
	private static void exportFolder(Connection conn, int folderId, Map<Integer, Map<Integer, String>> userLookup) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			
			String query = "select messageid,parentid,folderid,message_date,fromid,toid,message_text,subject,edf from folder_message_item where folderid=" + folderId;
			
			long minDate = System.currentTimeMillis() / 1000;
			minDate -= 3600;
			
			query += " and message_date >= " + minDate;
			query += " order by messageid";
			rs = stmt.executeQuery(query);
			while(rs.next()) {
				System.out.println(rs.getInt("messageid") + ":" + rs.getString("message_text"));
			}
		} finally {
			close(null, stmt, rs);
		}
	}

	public static void main(String[] args) {
		Connection conn = null;

		try {
			String db = "";
			String user = "";
			String password = "";
			
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost/" + db + "?user=" + user + " &password=" + password);

			Map<Integer, Map<Integer, String>> userLookup = getUserLookup(conn);
			
			Map<Integer, String> folderLookup = getFolderLookup(conn);
			
			for(int folderId : folderLookup.keySet()) {
				exportFolder(conn, folderId, userLookup);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(conn, null, null);
		}
	}	
}
