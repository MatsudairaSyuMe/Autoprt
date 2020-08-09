package com.systex.sysgateii.gateway.dao;

import java.beans.Statement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwDao {
	private static Logger log = LoggerFactory.getLogger(GwDao.class);
	// test
	private String selurl = "jdbc:db2://172.16.71.128:50000/BISDB";
	private String seluser = "BIS_USER";
	private String selpass = "bisuser";
	// test
	private Connection selconn = null;
	private PreparedStatement preparedStatement = null;
	private Vector<String> columnNames = null;
	private Vector<Integer> columnTypes = null;
	private ResultSet rs = null;
	private boolean verbose = true;
	private String sfn = "";

	/**
	 * 
	 */
	public GwDao() throws Exception {
		super();
		log.debug("using url:{} user:{} pass:{} start to connect to Database", this.selurl, this.seluser, this.selpass);
		selconn = getDB2Connection(selurl, seluser, selpass);
		log.debug("Connected to database successfully...");
	}

	public GwDao(String selurl, String seluser, String selpass, boolean v) throws Exception {
		super();
		this.selurl = selurl;
		this.seluser = seluser;
		this.selpass = selpass;
		log.debug("Connecting to a selected database...");
		selconn = getDB2Connection(selurl, seluser, selpass);
		log.debug("Connected selected database successfully...");
		this.verbose = v;
	}

	public int UPSERT(String fromTblName, String field, String updval, String keyname, String selkeyval)
			throws Exception {
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		// STEP 4: Execute a query
		if (fromTblName == null || fromTblName.trim().length() == 0)
			throw new Exception("given table name error =>" + fromTblName);
		log.debug(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval));
		java.sql.Statement stmt = selconn.createStatement();
		rs = ((java.sql.Statement) stmt)
				.executeQuery("SELECT " + field + " FROM " + fromTblName + " where " + keyname + "=" + selkeyval);
		String[] valary = updval.split(",");
		for (int i = 0; i < valary.length; i++) {
			int s = valary[i].indexOf('\'');
			int l = valary[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valary[i] = valary[i].substring(s + 1, l);
		}
		int type = -1;
		int row = 0;

		if (rs != null) {
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = 0;
			while (columnCount < rsmd.getColumnCount()) {
				columnCount++;
				type = rsmd.getColumnType(columnCount);
				if (verbose)
					log.debug("ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
				columnNames.add(rsmd.getColumnName(columnCount));
				columnTypes.add(type);
			}
			String colNames = "";
			String vals = "";
			String updcolNames = "";
			String updvals = "";
			log.debug("table fields {}", Arrays.toString(columnNames.toArray()));
			log.debug("given vals {}", Arrays.toString(valary));

			for (columnCount = 0; columnCount < columnNames.size(); columnCount++) {
				if (colNames.trim().length() > 0) {
					colNames = colNames + "," + columnNames.get(columnCount);
					vals = vals + ",?";
				} else {
					colNames = columnNames.get(columnCount);
					vals = "?";
				}
				if (!columnNames.get(columnCount).equalsIgnoreCase(keyname)) {
					if (updcolNames.trim().length() > 0) {
						updcolNames = updcolNames + "," + columnNames.get(columnCount);
						updvals = updvals + ",?";
					} else {
						updcolNames = columnNames.get(columnCount);
						updvals = "?";
					}
				}
			}
			String SQL_INSERT = "INSERT INTO " + fromTblName + " (" + colNames + ") VALUES (" + vals + ")";
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
					+ keyname + " = " + selkeyval;

			if (rs.next()) {
				preparedStatement = selconn.prepareStatement(SQL_UPDATE);
				log.debug("record exist using update:{}", SQL_UPDATE);
				setValueps(preparedStatement, valary, true);

			} else {
				preparedStatement = selconn.prepareStatement(SQL_INSERT);
				setValueps(preparedStatement, valary, false);
				log.debug("record not exist using insert:{}", SQL_INSERT);
			}

			row = preparedStatement.executeUpdate();

		}
		return row;
	}

	public String SELTBSDY(String fromTblName, String fieldn, String keyname, int keyvalue)
			throws Exception {
		String rtnVal = "";
		if (fromTblName == null || fromTblName.trim().length() == 0 || fieldn == null || fieldn.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			return rtnVal;
		try {
			java.sql.Statement stmt = selconn.createStatement();
			rs = ((java.sql.Statement) stmt).executeQuery("SELECT " + fieldn + " FROM " + fromTblName + " where "
					+ keyname + " = " + Integer.toString(keyvalue));
			if (rs != null) {
				if (rs.next()) {
					rtnVal = Integer.toString(rs.getInt(fieldn));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		log.debug("return TBSDY=[{}]", rtnVal);
		return rtnVal;
	}
	private PreparedStatement setValueps(PreparedStatement ps, String[] updvalary, boolean updinsert) throws Exception {
		// updinsert true for update, false for insert
		int type;
		String obj = "";
		int j = 1;
		if (!updinsert)
			j = 0;
		int i = 1;
		for (; j < columnNames.size(); j++) {
			type = columnTypes.get(j);
			obj = updvalary[j];
			if (verbose)
				log.debug("\t" + obj + ":");
			switch (type) {
			case Types.VARCHAR:
			case Types.CHAR:
				if (verbose)
					log.debug("rs.setString");
				ps.setString(i, obj);
				break;
			case Types.DECIMAL:
				if (verbose)
					log.debug("rs.setDouble");
				ps.setDouble(i, Double.parseDouble(obj));
				break;
			case Types.TIMESTAMP:
				if (verbose)
					log.debug("rs.setTimestamp");
				ps.setTimestamp(i, Timestamp.valueOf(obj));
				break;
			case Types.BIGINT:
				if (verbose)
					log.debug("rs.setLong");
				ps.setLong(i, Long.parseLong(obj));
				break;
			case Types.BLOB:
				if (verbose)
					log.debug("rs.setBlob");
				ps.setBlob(i, new javax.sql.rowset.serial.SerialBlob(obj.getBytes()));
				break;
			case Types.CLOB:
				if (verbose)
					log.debug("rs.setClob");
				Clob clob = ps.getConnection().createClob();
				clob.setString(1, obj);
				ps.setClob(i, clob);
				break;
			case Types.DATE:
				if (verbose)
					log.debug("rs.setDate");
				ps.setDate(i, Date.valueOf(obj));
				break;
			case Types.DOUBLE:
				if (verbose)
					log.debug("rs.setDouble");
				ps.setDouble(i, Double.valueOf(obj));
				break;
			case Types.INTEGER:
				if (verbose)
					log.debug("rs.setInt/getInt");
				ps.setInt(i, Integer.valueOf(obj));
				break;
			case Types.NVARCHAR:
				if (verbose)
					log.debug("rs.setNString(idx, v)");
				ps.setNString(i, obj);
				break;
			default:
				log.error("undevelop type:{} change to string", type);
				ps.setString(i, obj);
				break;
			}
			i += 1;
		}
		if (verbose)
			System.out.println();
		return ps;
	}

	public void CloseConnect() throws Exception {
		try {
			if (selconn != null)
				selconn.close();
		} catch (SQLException se) {
			se.printStackTrace();
			log.error("CloseConnect():{}", se.getMessage());
		} // end finally try
	}

	private Connection getDB2Connection(String url, String username, String password) throws Exception {
		Class.forName("com.ibm.db2.jcc.DB2Driver");
		log.debug("Driver Loaded.");
		return DriverManager.getConnection(url, username, password);
	}

	private Connection getHSQLConnection() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		System.out.println("Driver Loaded.");
		String url = "jdbc:hsqldb:data/tutorial";
		return DriverManager.getConnection(url, "sa", "");
	}

	private Connection getMySqlConnection() throws Exception {
		String driver = "org.gjt.mm.mysql.Driver";
		String url = "jdbc:mysql://localhost/demo2s";
		String username = "oost";
		String password = "oost";

		Class.forName(driver);
		Connection conn = DriverManager.getConnection(url, username, password);
		return conn;
	}

	private Connection getOracleConnection() throws Exception {
		String driver = "oracle.jdbc.driver.OracleDriver";
		String url = "jdbc:oracle:thin:@localhost:1521:caspian";
		String username = "mp";
		String password = "mp2";

		Class.forName(driver); // load Oracle driver
		Connection conn = DriverManager.getConnection(url, username, password);
		return conn;
	}

	/**
	 * @return the selurl
	 */
	public String getSelurl() {
		return selurl;
	}

	/**
	 * @param selurl the selurl to set
	 */
	public void setSelurl(String selurl) {
		this.selurl = selurl;
	}

	/**
	 * @return the seluser
	 */
	public String getSeluser() {
		return seluser;
	}

	/**
	 * @param seluser the seluser to set
	 */
	public void setSeluser(String seluser) {
		this.seluser = seluser;
	}

	/**
	 * @return the selpass
	 */
	public String getSelpass() {
		return selpass;
	}

	/**
	 * @param selpass the selpass to set
	 */
	public void setSelpass(String selpass) {
		this.selpass = selpass;
	}

	/**
	 * @return the sfn
	 */
	public String getSfn() {
		return sfn;
	}

	/**
	 * @param sfn the sfn to set
	 */
	public void setSfn(String sfn) {
		this.sfn = sfn;
	}

	public static void main(String[] args) {
		int total = 0;
		GwDao jsel2ins = null;
		String fromurl = "jdbc:db2://172.16.71.128:50000/BISDB";
		String fromuser = "BIS_USER";
		String frompass = "bisuser";

		boolean verbose = false;
		String fn = "BISAP.TB_AUDEVSTS";
		if (args.length > 0) {
			for (int j = 0; j < args.length; j++) {
				if (args[j].equalsIgnoreCase("--from")) {
					String[] fromary = args[j + 1].split(",");
					if (fromary.length > 0) {
						if (fromary.length == 1)
							fromurl = fromary[0];
						else if (fromary.length > 1) {
							fromurl = fromary[0];
							fromuser = fromary[1];
							frompass = fromary[2];
						}
					}
					System.out.println("from:" + fromurl + " " + fromuser + " " + frompass);
				} else if (args[j].equalsIgnoreCase("--verbose")) {
					verbose = true;
					System.out.println("verbose mode");
				} else if (args[j].equalsIgnoreCase("--tbn")) {
					fn = args[j + 1];
					System.out.println("dump table file:" + fn);
				}
			}
		}

		try {

			jsel2ins = new GwDao(fromurl, fromuser, frompass, verbose);

			String selField = "BRWS,IP,PORT,SYSIP,SYSPORT,ACTPAS,DEVTPE,CURSTUS,VERSION,CREATOR,MODIFIER";
			String updValue = "'9838901',10.24.1.230,'4002','10.24.1.230','3301','0','3','0','1','SYSTEM',''";
			String keyName = "BRWS";
			String keyValue = "9838901";

			total = 0;
//			total = jsel2ins.UPSERT(fn, selField, updValue, keyName, keyValue);
//			System.out.println("total " + total + " records transferred");
			System.out.println("TBSDY= " + jsel2ins.SELTBSDY("BISAP.TB_AUSVRPRM", "TBSDY", "SVRID", 1));


		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			// finally block used to close resources
			try {
				if (jsel2ins != null)
					jsel2ins.CloseConnect();
			} catch (Exception e) {
			}
		} // end try

		System.out.println("Goodbye!");
	}// en
}
