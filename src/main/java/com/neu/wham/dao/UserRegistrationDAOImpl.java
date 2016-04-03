package com.neu.wham.dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.neu.wham.keys.DBConstants;
import com.neu.wham.model.User;
import com.neu.wham.services.UserRegistrationService;

@Repository
public class UserRegistrationDAOImpl implements UserRegistrationDAO {
	
	static{
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public User createNewUser(User user) throws SQLException, NoSuchAlgorithmException {
		Connection conn = null;
		conn = DriverManager.getConnection(DBConstants.DB_URL,DBConstants.USER,DBConstants.PASS);
		
		//TODO: encrypt Password
        user.setPassword(encryptPassword(user.getPassword()));
		
		String sql_statement = "insert into USER(first_name,middle_name,last_name,emailId,"
				+ "phone_no,password)"
				+ "values(?,?,?,?,?,?);";
		PreparedStatement stmt =conn.prepareStatement(sql_statement);
		stmt.setString(1, user.getFirstName());
		stmt.setString(2, user.getMiddleName());
		stmt.setString(3, user.getLastName());
		stmt.setString(4, user.getEmailId());
		stmt.setString(5, user.getPhoneNo());
		stmt.setString(6, user.getPassword());

		try{
			int val = stmt.executeUpdate();
			if(val!=0){
				return user;
			}
			}catch(Exception e){
				e.printStackTrace();
				throw e;
			}
		throw new SQLException();
	}

	@Override
	public User validateUser(String emailId, String password) throws SQLException, NoSuchAlgorithmException {
		User user = null;
		Connection conn = null;
		conn = DriverManager.getConnection(DBConstants.DB_URL,DBConstants.USER,DBConstants.PASS);
		String sql_statement = "select * from USER where emailId = ?;";

		PreparedStatement ppdStmt = conn.prepareStatement(sql_statement);
		ppdStmt.setString(1, emailId);
		
		ResultSet rs = ppdStmt.executeQuery();

		if (rs.next()) {
			user = new User();
			user.setUserId(rs.getInt("user_id"));
			user.setFirstName(rs.getString("first_name"));
			user.setMiddleName(rs.getString("middle_name"));
			user.setLastName(rs.getString("last_name"));
			user.setPhoneNo(rs.getString("phone_no"));
			user.setEmailId(rs.getString("emailId"));
			user.setPassword(rs.getString("password"));
			if(user.getPassword().equals(encryptPassword(password)))
				return user;
		}
		
		return null;
	}
	
	private String encryptPassword(String password) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(password.getBytes());
		byte byteData[] = md.digest();
        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
         sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
	}
}