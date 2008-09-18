package rsv.process.test;

import java.sql.*;

public class JDBCConnection implements Base {

	public static void main(String[] args) {
		 JDBCConnection test = new JDBCConnection();
		 test.run();
	}

	public void run() {
		System.out.println("Test JDBC Connection");	
	    try {
	    	//connect to mysql
	        String url = "jdbc:mysql://rsv-itb.grid.iu.edu:49152/rsvextra";
	        Connection con = DriverManager.getConnection(url,"rsv-view-write", "soichisviews");
	        System.out.println("URL: " + url);
	        System.out.println("Connection: " + con);
	        
	        //run something
	        Statement stmt = con.createStatement();
	        ResultSet rs = stmt.executeQuery("select * from oim.resource");
	        while(rs.next()){
	            int resource_id = rs.getInt("resource_id");
	            String name = rs.getString("name");
	            System.out.println(resource_id + " " + name);
	        }//end while loop

	        //close connection
	        con.close();
	        System.out.println("Connection Closed");
	        
	    } catch( Exception e ) {
	            e.printStackTrace();  
	    }
	}
}
