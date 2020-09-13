package com.systex.sysgateii.gateway.dao;

import java.sql.Blob;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.gateway.autoPrtSvr.Server.PrnSvr;
import com.systex.sysgateii.gateway.comm.Constants;
import com.systex.sysgateii.gateway.util.DataConvert;

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
	//20200815
	private PreparedStatement tbsdytblpreparedStatement = null;
	private Vector<String> tbsdytblcolumnNames = null;
	private Vector<Integer> tbsdytblcolumnTypes = null;
	private ResultSet tbsdytblrs = null;
	private String tbsdysfn = "";
	//----

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
//		log.debug("Connecting to a selected database...");
		selconn = getDB2Connection(selurl, seluser, selpass);
//		log.debug("Connected selected database successfully...");
		this.verbose = v;
	}

	public int UPSERT2(String fromTblName, String field, String updval, String keyname, String selkeyval)
			throws Exception {
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		// STEP 4: Execute a query
		//20200908 add check for field and keyname
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval));
		java.sql.Statement stmt = selconn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		//20200908
		String keyset = "";
		if (keyname.indexOf(',') > -1 && selkeyval.indexOf(',') > -1) {
			String[] keynameary = keyname.split(",");
			String[] keyvalueary = selkeyval.split(",");
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkayvals [" + selkeyval + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
			}
		} else
			keyset = keyname + " = " + selkeyval;
		//----
        //20200908 modify for multiple fields key value
//		rs = ((java.sql.Statement) stmt)
//				.executeQuery("SELECT " + field + " FROM " + fromTblName + " where " + keyname + "=" + selkeyval);
		rs = ((java.sql.Statement) stmt)
				.executeQuery("SELECT " + field + " FROM " + fromTblName + " where " + keyset);
		//----
		log.debug("update value [{}]", updval);
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
			//20200908 modify for multi key value
//			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
//					+ keyname + " = " + selkeyval;
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
					+ keyset;
			log.debug("given SQL_INSERT = {}", SQL_INSERT);
			log.debug("given SQL_UPDATE = {}", SQL_UPDATE);
			//----
			if (rs.next()) {
				preparedStatement = selconn.prepareStatement(SQL_UPDATE);
				log.debug("record exist using update:{}", SQL_UPDATE);
				log.debug("record exist using valary:{} len={}", valary, valary.length);
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
	public int UPSERT(String fromTblName, String field, String updval, String keyname, String selkeyval)
			throws Exception {
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval));
		java.sql.Statement stmt = selconn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		//20200908
		String keyset = "";
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		if (keynameary.length != keyvalueary.length)
			throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkayvals [" + selkeyval + "]");
		else {
			for (int i = 0; i < keynameary.length; i++)
				keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
		}
		rs = ((java.sql.Statement) stmt)
				.executeQuery("SELECT " + keyname + "," + field + " FROM " + fromTblName + " where " + keyset);
		log.debug("update value [{}] selkeyval [{}]", updval, selkeyval);
		String[] valary = updval.split(",");
		for (int i = 0; i < valary.length; i++) {
			int s = valary[i].indexOf('\'');
			int l = valary[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valary[i] = valary[i].substring(s + 1, l);
		}
		String[] selkeyvalary = selkeyval.split(",");
		for (int i = 0; i < selkeyvalary.length; i++) {
			int s = selkeyvalary[i].indexOf('\'');
			int l = selkeyvalary[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				selkeyvalary[i] = selkeyvalary[i].substring(s + 1, l);
		}
		int type = -1;
		int row = 0;

		if (rs != null) {
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = 0;
			boolean updateMode = false;
			log.debug("table request fields {}", field);
			if (rs.next()) {
				log.debug("update mode");
				updateMode = true;
			} else
				log.debug("insert mode");
			while (columnCount < rsmd.getColumnCount()) {
				columnCount++;
				type = rsmd.getColumnType(columnCount);
				if (updateMode && field.indexOf(rsmd.getColumnName(columnCount).trim()) > -1) {
					columnNames.add(rsmd.getColumnName(columnCount));
					columnTypes.add(type);
					if (verbose)
						log.debug("updateMode ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
				} else if (!updateMode && (field.indexOf(rsmd.getColumnName(columnCount).trim()) > -1 || keyname.indexOf(rsmd.getColumnName(columnCount).trim()) > -1)) {
					columnNames.add(rsmd.getColumnName(columnCount));
					columnTypes.add(type);					
					if (verbose)
						log.debug("insert Mode ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
				}
			}
			String colNames = "";
			String vals = "";
			String updcolNames = "";
			String updvals = "";
			log.debug("given vals {} selkeyvalary {}", Arrays.toString(valary), Arrays.toString(selkeyvalary));

			for (columnCount = 0; columnCount < columnNames.size(); columnCount++) {
				if (colNames.trim().length() > 0) {
					colNames = colNames + "," + columnNames.get(columnCount);
					vals = vals + ",?";
				} else {
					colNames = columnNames.get(columnCount);
					vals = "?";
				}
				if (updateMode) {
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
			//20200908 modify for multi key value
//			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
//					+ keyname + " = " + selkeyval;
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
					+ keyset;
			//----
			if (updateMode) {
				preparedStatement = selconn.prepareStatement(SQL_UPDATE);
				log.debug("record exist using update:{}", SQL_UPDATE);
				log.debug("record exist using valary:{} len={}", valary, valary.length);
				setValueps(preparedStatement, valary, false);

			} else {
				preparedStatement = selconn.prepareStatement(SQL_INSERT);
				String[] insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(selkeyvalary, valary);
				setValueps(preparedStatement, insvalary, false);
				log.debug("record not exist using insert:{}", SQL_INSERT);
			}

			row = preparedStatement.executeUpdate();

		}
		return row;
	}
	//20200908 update only
	public int UPDT(String fromTblName, String field, String updval, String keyname, String selkeyval)
			throws Exception {
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		// STEP 4: Execute a query
		//20200908 add check for field and keyname
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval));
		java.sql.Statement stmt = selconn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		//20200908
		String keyset = "";
		if (keyname.indexOf(',') > -1 && selkeyval.indexOf(',') > -1) {
			String[] keynameary = keyname.split(",");
			String[] keyvalueary = selkeyval.split(",");
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkayvals [" + selkeyval + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
			}
		} else
			keyset = keyname + " = " + selkeyval;
		//----
        //20200908 modify for multiple fields key value
//		rs = ((java.sql.Statement) stmt)
//				.executeQuery("SELECT " + field + " FROM " + fromTblName + " where " + keyname + "=" + selkeyval);
		rs = ((java.sql.Statement) stmt)
				.executeQuery("SELECT " + field + " FROM " + fromTblName + " where " + keyset);
		//----
		log.debug("update value [{}]", updval);
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
			//20200908 modify for multi key value
//			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
//					+ keyname + " = " + selkeyval;
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
					+ keyset;
			//----
			if (rs.next()) {
				preparedStatement = selconn.prepareStatement(SQL_UPDATE);
				log.debug("record exist using update:{}", SQL_UPDATE);
				log.debug("record exist using valary:{} len={}", valary, valary.length);
				setValueps(preparedStatement, valary, false);

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
			tbsdytblrs = ((java.sql.Statement) stmt).executeQuery("SELECT " + fieldn + " FROM " + fromTblName + " where "
					+ keyname + " = " + Integer.toString(keyvalue));
			if (tbsdytblrs != null) {
				if (tbsdytblrs.next()) {
//					rtnVal = Integer.toString(tbsdytblrs.getInt(fieldn));
					rtnVal = tbsdytblrs.getString(fieldn);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		log.debug("return TBSDY=[{}]", rtnVal);
		return rtnVal;
	}
	//20200815
	public String SELONEFLD(String fromTblName, String fieldn, String keyname, String keyvalue, boolean verbose)
			throws Exception {
		String rtnVal = "";
		tbsdytblcolumnNames = new Vector<String>();
		tbsdytblcolumnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || fieldn == null || fieldn.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			return rtnVal;
		try {
			log.debug("keyname = keyvalue=[{}]",  keyname + "=" + keyvalue);

			//20200901

			String keyset = "";
			if (keyname.indexOf(',') > -1 && keyvalue.indexOf(',') > -1) {
				String[] keynameary = keyname.split(",");
				String[] keyvalueary = keyvalue.split(",");
				if (keynameary.length != keyvalueary.length)
					return rtnVal;
				else {
					for (int i = 0; i < keynameary.length; i++)
						keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
				}
			} else
				keyset = keyname + " = " + keyvalue;

			//----
			java.sql.Statement stmt = selconn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
//20200901 change to use key set
//			tbsdytblrs = ((java.sql.Statement) stmt).executeQuery("SELECT " + fieldn + " FROM " + fromTblName + " where "
//					+ keyname + "=" + keyvalue);

			tbsdytblrs = ((java.sql.Statement) stmt).executeQuery("SELECT " + fieldn + " FROM " + fromTblName + " where "
					+ keyset);

			int type = -1;
			log.debug("ketset=[{}]", keyset);

			if (tbsdytblrs != null) {
				ResultSetMetaData rsmd = tbsdytblrs.getMetaData();
				int columnCount = 0;
				while (columnCount < rsmd.getColumnCount()) {
					columnCount++;
					type = rsmd.getColumnType(columnCount);
					if (verbose)
						log.debug("ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
					tbsdytblcolumnNames.add(rsmd.getColumnName(columnCount));
					tbsdytblcolumnTypes.add(type);
				}
				//20200901
				int idx = 0;
				while (tbsdytblrs.next()) {
//					log.debug("-->[{}]",gettbsdytblValue(tbsdytblrs, fieldn, verbose));
					if (idx == 0)
						rtnVal = tbsdytblrs.getString(fieldn);
					else
						rtnVal = rtnVal + "," + tbsdytblrs.getString(fieldn);
					idx++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		log.debug("return SELONEFLD=[{}]", rtnVal);
		return rtnVal;
	}
	//----
	//20200901
	public String[] SELMFLD(String fromTblName, String fieldsn, String keyname, String keyvalue, boolean verbose)
			throws Exception {
		String[] rtnVal = {};
		tbsdytblcolumnNames = new Vector<String>();
		tbsdytblcolumnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || fieldsn == null || fieldsn.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			return rtnVal;
		try {
			log.debug("fieldsn=[{}] keyname = keyvalue : [{}]",  fieldsn, keyname + "=" + keyvalue);
			String[] fieldset = null;
			if (fieldsn.indexOf(',') > -1)
				fieldset = fieldsn.split(",");
			else {
				fieldset = new String[1];
				fieldset[0] = fieldsn;
			}
			String keyset = "";
			if (keyname.indexOf(',') > -1 && keyvalue.indexOf(',') > -1) {
				String[] keynameary = keyname.split(",");
				String[] keyvalueary = keyvalue.split(",");
				if (keynameary.length != keyvalueary.length)
					return rtnVal;
				else {
					for (int i = 0; i < keynameary.length; i++)
						keyset = keyset + keynameary[i] + " = " + keyvalueary[i] + (i == (keynameary.length - 1) ? "" : " and ");
				}
			} else
				keyset = keyname + " = " + keyvalue;
			java.sql.Statement stmt = selconn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
			tbsdytblrs = ((java.sql.Statement) stmt).executeQuery("SELECT " + fieldsn + " FROM " + fromTblName + " where "
					+ keyset);

			int type = -1;
			log.debug("ketset=[{}]", keyset);

			if (tbsdytblrs != null) {
				ResultSetMetaData rsmd = tbsdytblrs.getMetaData();
				int columnCount = 0;
				while (columnCount < rsmd.getColumnCount()) {
					columnCount++;
					type = rsmd.getColumnType(columnCount);
					if (verbose)
						log.debug("ColumnName={} ColumnTypeName={} ", rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount) );
					tbsdytblcolumnNames.add(rsmd.getColumnName(columnCount));
					tbsdytblcolumnTypes.add(type);
				}
				int idx = 0;
				while (tbsdytblrs.next()) {
					if (idx <= 0)
						rtnVal = new String[1];
					else {
						String[] tmpv = rtnVal;
						rtnVal = new String[idx + 1];
						int j = 0;
						for (String s: tmpv) {
							rtnVal[j] = s;
							j++;
						}
					}
					for (int i = 0; i < fieldset.length; i++) {
//						log.debug("i={}-->[{}]",i, gettbsdytblValue(tbsdytblrs, fieldset[i], verbose));
						if (i == 0)
							rtnVal[idx] = tbsdytblrs.getString(fieldset[i]);
						else
							rtnVal[idx] = rtnVal[idx] + "," + tbsdytblrs.getString(fieldset[i]);
					}
					idx++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		log.debug("return SELMFLD length=[{}]", rtnVal.length);
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
//		log.debug("j={} columnNames.size()={}",j,columnNames.size());
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
					log.debug("rs.setTimestamp[{}]", obj);
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
	private String gettbsdytblValue(ResultSet rs, String obj, boolean verbose) throws Exception {
		int type;
		String rtn = "";
		for (int j = 0; j < tbsdytblcolumnNames.size(); j++) {
			if (!tbsdytblcolumnNames.get(j).endsWith(obj))
				continue;
			type = tbsdytblcolumnTypes.get(j);
			if (verbose)
				log.debug("\t" + obj + ":");
			switch (type) {
			case Types.VARCHAR:
			case Types.CHAR:
				if (verbose)
					log.debug("rs.getString");
				rtn = rs.getString(obj);
				break;
			case Types.DECIMAL:
				if (verbose)
					log.debug("rs.getDouble");
				rtn = Double.toString(rs.getDouble(obj));
				break;
			case Types.TIMESTAMP:
				if (verbose)
					log.debug("rs.getTimestamp");
				rtn = rs.getTimestamp(obj).toString();
				break;
			case Types.BIGINT:
				if (verbose)
					log.debug("rs.getLong");
				rtn = Long.toString(rs.getLong(obj));
				break;
			case Types.BLOB:
				if (verbose)
					log.debug("rs.getBlob");
				Blob blob = rs.getBlob(obj);
				int blobLength = (int) blob.length();
				byte[] blobAsBytes = blob.getBytes(1, blobLength);
				rtn  = DataConvert.bytesToHex(blobAsBytes);
				break;
			case Types.CLOB:
				if (verbose)
					log.debug("rs.getClob");
				Clob clob = rs.getClob(obj);
				rtn = clob.toString();
				break;
			case Types.DATE:
				if (verbose)
					log.debug("rs.getDate");
				rtn = rs.getString(obj);
				break;
			case Types.DOUBLE:
				if (verbose)
					log.debug("rs.getDouble");
				rtn = Double.toString(rs.getDouble(obj));
				break;
			case Types.INTEGER:
				if (verbose)
					log.debug("rs.getInt");
				rtn = Integer.toString(rs.getInt(obj));
				break;
			case Types.NVARCHAR:
				if (verbose)
					log.debug("rs.getNString(idx, v)");
				rtn = rs.getNString(obj);
				break;
			default:
				log.error("undevelop type:{} change to string", type);
				rtn = rs.getString(obj);
				break;
			}
			break;
		}
		return rtn;
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

			jsel2ins = null;

			String selField = "IP,PORT,SYSIP,SYSPORT,ACTPAS,DEVTPE,CURSTUS,VERSION,CREATOR,MODIFIER";
			String updValue = "'10.24.1.230','4002','10.24.1.230','3301','0','3','0','1','SYSTEM',''";
			String keyName = "BRWS";
			String keyValue = "9838901";

			total = 0;
			
/*			while (true) {
//				if (jsel2ins == null)
//					jsel2ins = new GwDao(fromurl, fromuser, frompass, verbose);
//				total = jsel2ins.UPSERT(fn, selField, updValue, keyName, keyValue);
//				jsel2ins.CloseConnect();
//				jsel2ins = null;
//			System.out.println("total " + total + " records transferred");
//			System.out.println("TBSDY= " + jsel2ins.SELTBSDY("BISAP.TB_AUSVRPRM", "TBSDY", "SVRID", 1));
//			System.out.println("TBSDY= " + jsel2ins.SELONEFLD("BISAP.TB_AUSVRPRM", "SVRID,TBSDY", "SVRID", "1", false));
				if (jsel2ins == null)
					jsel2ins = new GwDao(fromurl, fromuser, frompass, verbose);
				System.out.println("TBSDY= " + jsel2ins.SELONEFLD("BISAP.TB_AUSVRPRM", "TBSDY", "SVRID", "1", false));
				jsel2ins.CloseConnect();
				jsel2ins = null;
				Thread.sleep(2 * 1000);
			}
			*/
			
			if (jsel2ins == null)
				jsel2ins = new GwDao(fromurl, fromuser, frompass, verbose);
//			total = jsel2ins.UPSERT(fn, selField, updValue, keyName, keyValue);
			total = jsel2ins.UPSERT("BISAP.TB_AUSVRPRM", "TBSDY", "01090910", "SVRID", "1");
			jsel2ins.CloseConnect();
			jsel2ins = null;
			System.out.println("total " + total + " records transferred");
			
/*
			if (jsel2ins == null)
				jsel2ins = new GwDao(fromurl, fromuser, frompass, verbose);
			String tb = "BISAP.TB_AUDEVCMD";
			System.out.println("table " + tb);
			String[] cmd = jsel2ins.SELMFLD(tb, "SVRID,CMD", "SVRID", "1", true);
			for (String c: cmd)
				System.out.println(" cmd= " + c);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			String t = sdf.format(new java.util.Date());

			updValue = "'','START','" + t + "'";

			int row = jsel2ins.UPDT(tb, "CMD, CMDRESULT,CMDRESULTTIME", updValue, "SVRID,BRWS", "1,9838901");
			log.debug("total {} records update  status [{}]", row, Constants.STSUSEDINACT);

			jsel2ins.CloseConnect();
			jsel2ins = null;
*/
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
