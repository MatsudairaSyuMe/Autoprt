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
	private Vector<String> tbsdytblcolumnNames = null;
	private Vector<Integer> tbsdytblcolumnTypes = null;
	private ResultSet tbsdytblrs = null;
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

	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public int UPSERT(String fromTblName, String field, String updval, String keyname, String selkeyval)
			throws Exception {
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("Select from table %s... where %s=%s", fromTblName, keyname, selkeyval));
		String keyset = "";
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		String[] keyvaluearynocomm = selkeyval.split(",");
		if (keynameary.length != keyvalueary.length)
			throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkeyval [" + selkeyval + "]");
		else {
			for (int i = 0; i < keynameary.length; i++)
				keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");
			for (int i = 0; i < keyvaluearynocomm.length; i++) {
				int s = keyvalueary[i].indexOf('\'');
				int l = keyvalueary[i].lastIndexOf('\'');
				if (s != l && s >= 0 && l >= 0 && s < l)
					keyvaluearynocomm[i] = keyvalueary[i].substring(s + 1, l);
			}
		}
		String selstr = "SELECT " + keyname + "," + field + " FROM " + fromTblName + " where " + keyset;
		log.debug("UPSERT selstr [{}]", selstr);
		log.debug("update value [{}]", updval);
		String[] valary = updval.split(",");
		for (int i = 0; i < valary.length; i++) {
			int s = valary[i].indexOf('\'');
			int l = valary[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valary[i] = valary[i].substring(s + 1, l);
		}

		PreparedStatement stmt = selconn.prepareStatement(selstr);
		for (int i = 0; i < keyvalueary.length; i++) {
			if (keyvalueary[i].indexOf('\'') > -1 )
				stmt.setString(i + 1, keyvaluearynocomm[i]);
			else
				stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
		}
		ResultSet tblrs = stmt.executeQuery();
		int type = -1;
		int row = 0;
		//verbose=true;
		if (tblrs != null) {
			ResultSetMetaData rsmd = tblrs.getMetaData();
			int columnCount = 0;
			boolean updateMode = false;
			log.debug("table request fields {}", field);
			if (tblrs.next()) {
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
			log.debug("given vals {} keyvaluearynocomm {}", Arrays.toString(valary), Arrays.toString(keyvaluearynocomm));

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
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
					+ keyset;
			String[] insvalary = null;
			if (updateMode) {
				for (int i = 0; i < keynameary.length; i++) {
					columnCount++;
					if (verbose)
						log.debug("columnCount=[{}] ColumnName={} ColumnTypeName={} ", columnCount, keynameary[i], keyvalueary[i].indexOf('\'') > -1? "VARCHAR":"INTEGER");
					columnNames.add(keynameary[i]);
					if (keyvalueary[i].indexOf('\'') > -1)
						columnTypes.add(Types.VARCHAR);
					else
						columnTypes.add(Types.INTEGER);
				}
				insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(valary, keyvaluearynocomm);
				preparedStatement = selconn.prepareStatement(SQL_UPDATE);
				log.debug("record exist using update:{}", SQL_UPDATE);
				log.debug("record exist using valary:{} len={}", insvalary, insvalary.length);
				setValueps(preparedStatement, insvalary, false);

			} else {
				preparedStatement = selconn.prepareStatement(SQL_INSERT);
				log.debug("record not exist using insert:{}", SQL_INSERT);
				insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(keyvaluearynocomm, valary);
				setValueps(preparedStatement, insvalary, false);
			}
			row = preparedStatement.executeUpdate();
			tblrs.close();
		}
		return row;
	}
	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
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
		
		String keyset = "";
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		String[] keyvaluearynocomm = selkeyval.split(",");
		log.debug("update value [{}]", updval);
		String[] valary = updval.split(",");
		for (int i = 0; i < valary.length; i++) {
			int s = valary[i].indexOf('\'');
			int l = valary[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valary[i] = valary[i].substring(s + 1, l);
		}
		
		if (keynameary.length != keyvalueary.length)
			throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkeyval [" + selkeyval + "]");
		else {
			for (int i = 0; i < keynameary.length; i++)
				keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");
			for (int i = 0; i < keyvaluearynocomm.length; i++) {
				int s = keyvalueary[i].indexOf('\'');
				int l = keyvalueary[i].lastIndexOf('\'');
				if (s != l && s >= 0 && l >= 0 && s < l)
					keyvaluearynocomm[i] = keyvalueary[i].substring(s + 1, l);
			}
		}
		String selstr = "SELECT " + field + " FROM " + fromTblName + " where " + keyset;
		log.debug("UPDT selstr [{}]", selstr);
		PreparedStatement stmt = selconn.prepareStatement(selstr);
		for (int i = 0; i < keyvalueary.length; i++) {
			if (keyvalueary[i].indexOf('\'') > -1 )
				stmt.setString(i + 1, keyvaluearynocomm[i]);
			else
				stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
		}
		ResultSet tblrs = stmt.executeQuery();
		int type = -1;
		int row = 0;

		if (tblrs != null) {
			ResultSetMetaData rsmd = tblrs.getMetaData();
			int columnCount = 0;
			while (columnCount < rsmd.getColumnCount()) {
				columnCount++;
				type = rsmd.getColumnType(columnCount);
				if (verbose)
					log.debug("columnCount=[{}] ColumnName={} ColumnTypeName={} ", columnCount, rsmd.getColumnName(columnCount), rsmd.getColumnTypeName(columnCount));
				columnNames.add(rsmd.getColumnName(columnCount));
				columnTypes.add(type);
			}
			String colNames = "";
			String vals = "";
			String updcolNames = "";
			String updvals = "";
			log.debug("table fields {}", Arrays.toString(columnNames.toArray()));
			log.debug("given keyvaluearynocomm {}", Arrays.toString(keyvaluearynocomm));

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
					+ keyset;
			//----
			String[] insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(valary, keyvaluearynocomm);
			if (tblrs.next()) {
				for (int i = 0; i < keynameary.length; i++) {
					columnCount++;
					if (verbose)
						log.debug("columnCount=[{}] ColumnName={} ColumnTypeName={} ", columnCount, keynameary[i], keyvalueary[i].indexOf('\'') > -1? "VARCHAR":"INTEGER");
					columnNames.add(keynameary[i]);
					if (keyvalueary[i].indexOf('\'') > -1)
						columnTypes.add(Types.VARCHAR);
					else
						columnTypes.add(Types.INTEGER);
				}
				preparedStatement = selconn.prepareStatement(SQL_UPDATE);
				setValueps(preparedStatement, insvalary, false);
				log.debug("record exist using update:{}", SQL_UPDATE);
				log.debug("record exist using valary:{} len={}", insvalary, insvalary.length);
			} else {
				preparedStatement = selconn.prepareStatement(SQL_INSERT);
				setValueps(preparedStatement, insvalary, false);
				log.debug("record not exist using insert:{}", SQL_INSERT);
			}
			row = preparedStatement.executeUpdate();
			tblrs.close();
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
				tbsdytblrs.close();	
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		log.debug("return TBSDY=[{}]", rtnVal);
		return rtnVal;
	}
	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
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
			String keyset = "";
			String[] keynameary = keyname.split(",");
			String[] keyvalueary = keyvalue.split(",");
			String[] keyvaluearynocomm = keyvalue.split(",");
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] keyvalues [" + keyvalue + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");
				for (int i = 0; i < keyvaluearynocomm.length; i++) {
					int s = keyvalueary[i].indexOf('\'');
					int l = keyvalueary[i].lastIndexOf('\'');
					if (s != l && s >= 0 && l >= 0 && s < l)
						keyvaluearynocomm[i] = keyvalueary[i].substring(s + 1, l);
				}
			}

			if ((keyname.indexOf(',') > -1) && (keyvalue.indexOf(',') > -1)
					&& (keynameary.length != keyvalueary.length))
				return rtnVal;
			String selstr = "SELECT " + fieldn + " FROM " + fromTblName + " where " + keyset;
			log.debug("selstr=[{}]", selstr);
			PreparedStatement stmt = selconn.prepareStatement(selstr);
			for (int i = 0; i < keyvalueary.length; i++) {
				if (keyvalueary[i].indexOf('\'') > -1 )
					stmt.setString(i + 1, keyvaluearynocomm[i]);
				else
					stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
			}
			ResultSet tblrs = stmt.executeQuery();
			
			int type = -1;
			if (tblrs != null) {
				ResultSetMetaData rsmd = tblrs.getMetaData();
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
				while (tblrs.next()) {
					if (idx == 0)
						rtnVal = tblrs.getString(fieldn);
					else
						rtnVal = rtnVal + "," + tblrs.getString(fieldn);
					idx++;
				}
				tblrs.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		log.debug("return SELONEFLD=[{}]", rtnVal);
		return rtnVal;
	}
	//----
	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
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
			String[] keynameary = keyname.split(",");
			String[] keyvalueary = keyvalue.split(",");
			String[] keyvaluearynocomm = keyvalue.split(",");
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] keyvalues [" + keyvalue + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");
				for (int i = 0; i < keyvaluearynocomm.length; i++) {
					int s = keyvalueary[i].indexOf('\'');
					int l = keyvalueary[i].lastIndexOf('\'');
					if (s != l && s >= 0 && l >= 0 && s < l)
						keyvaluearynocomm[i] = keyvalueary[i].substring(s + 1, l);
				}
			}

			if ((keyname.indexOf(',') > -1) && (keyvalue.indexOf(',') > -1)
					&& (keynameary.length != keyvalueary.length))
				return rtnVal;

			String selstr = "SELECT " + fieldsn + " FROM " + fromTblName + " where " + keyset;
			log.debug("selstr=[{}]", selstr);
			PreparedStatement stmt = selconn.prepareStatement(selstr);
			for (int i = 0; i < keyvalueary.length; i++) {
				if (keyvalueary[i].indexOf('\'') > -1 )
					stmt.setString(i + 1, keyvaluearynocomm[i]);
				else
					stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
			}
			ResultSet tblrs = stmt.executeQuery();
			
			int type = -1;

			if (tblrs != null) {
				ResultSetMetaData rsmd = tblrs.getMetaData();
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
				while (tblrs.next()) {
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
						if (i == 0)
							rtnVal[idx] = tblrs.getString(fieldset[i]);
						else
							rtnVal[idx] = rtnVal[idx] + "," + tblrs.getString(fieldset[i]);
					}
					idx++;
				}
				tblrs.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		if (verbose)
			log.debug("return SELMFLD length=[{}]", rtnVal.length);
		return rtnVal;
	}

	//20201019
	public String[] SELMFLDNOIDX(String fromTblName, String fieldsn, boolean verbose)
			throws Exception {
		String[] rtnVal = {};
		tbsdytblcolumnNames = new Vector<String>();
		tbsdytblcolumnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || fieldsn == null || fieldsn.trim().length() == 0)
			return rtnVal;
		try {
			log.debug("fieldsn=[{}]",  fieldsn);
			String[] fieldset = null;
			if (fieldsn.indexOf(',') > -1)
				fieldset = fieldsn.split(",");
			else {
				fieldset = new String[1];
				fieldset[0] = fieldsn;
			}
			java.sql.Statement stmt = selconn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
			tbsdytblrs = ((java.sql.Statement) stmt).executeQuery("SELECT " + fieldsn + " FROM " + fromTblName);

			int type = -1;

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
						if (i == 0)
							rtnVal[idx] = tbsdytblrs.getString(fieldset[i]);
						else
							rtnVal[idx] = rtnVal[idx] + "," + tbsdytblrs.getString(fieldset[i]);
					}
					idx++;
				}
				tbsdytblrs.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error : {}", e.toString());
		}
		log.debug("return SELMFLDNOIDX length=[{}]", rtnVal.length);
		return rtnVal;
	}

	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public String[] INSSELChoiceKey(String fromTblName, String field, String selupdval, String keyname, String selkeyval, boolean usekey, boolean verbose) throws Exception {
		String[] rtnVal = null;
		columnNames = new Vector<String>();
		columnTypes = new Vector<Integer>();
		if (fromTblName == null || fromTblName.trim().length() == 0 || field == null || field.trim().length() == 0
				|| keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("first test Select from table %s... where %s=%s", fromTblName, keyname, selkeyval));
		String keyset = "";
		String selstr = "";
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		String[] keyvaluearynocomm = selkeyval.split(",");
		if (keynameary.length != keyvalueary.length)
			throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkayvals [" + selkeyval + "]");
		else {
			for (int i = 0; i < keynameary.length; i++)
				keyset = keyset + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");			for (int i = 0; i < keyvaluearynocomm.length; i++) {
				int s = keyvaluearynocomm[i].indexOf('\'');
				int l = keyvaluearynocomm[i].lastIndexOf('\'');
				if (s != l && s >= 0 && l >= 0 && s < l)
					keyvaluearynocomm[i] = keyvaluearynocomm[i].substring(s + 1, l);
			}
		}

		if (usekey) {
			selstr = "SELECT " + keyname + "," + field + " FROM " + fromTblName + " where " + keyset;
		} else {
			selstr = "SELECT " + field + " FROM " + fromTblName + " where " + keyset;
		}
		log.debug("sqlstr=[{}] selupdval value [{}] selkeyval [{}]", selstr, selupdval, selkeyval);
		String[] valary = selupdval.split(",");
		String[] valarynocomm = selupdval.split(",");
		for (int i = 0; i < valary.length; i++) {
			int s = valarynocomm[i].indexOf('\'');
			int l = valarynocomm[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valarynocomm[i] = valarynocomm[i].substring(s + 1, l);
		}
		int type = -1;
		int row = 0;

		PreparedStatement stmt = selconn.prepareStatement(selstr);
		for (int i = 0; i < keyvalueary.length; i++) {
			if (keyvalueary[i].indexOf('\'') > -1 )
				stmt.setString(i + 1, keyvaluearynocomm[i]);
			else
				stmt.setInt(i + 1, Integer.valueOf(keyvaluearynocomm[i]));
		}
		ResultSet tblrs = stmt.executeQuery();

		if (tblrs != null) {
			ResultSetMetaData rsmd = tblrs.getMetaData();
			int columnCount = 0;
			boolean updateMode = false;
			log.debug("table request fields {}", field);
			if (tblrs.next()) {
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
			log.debug("given vals {} selkeyvalary {}", Arrays.toString(valary), Arrays.toString(keyvaluearynocomm));

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
			String SQL_INSERT = "SELECT " + keyname + " FROM NEW TABLE (INSERT INTO " + fromTblName + " (" + colNames + ") VALUES (" + vals + "))";
			String SQL_UPDATE = "UPDATE " + fromTblName + " SET (" + updcolNames + ") = (" + updvals + ") WHERE "
					+ keyset;
			String cnvInsertStr = "";
			String[] insvalary = null;
			if (updateMode) {
				for (int i = 0; i < keynameary.length; i++) {
					columnCount++;
					if (verbose)
						log.debug("columnCount=[{}] ColumnName={} ColumnTypeName={} ", columnCount, keynameary[i], keyvalueary[i].indexOf('\'') > -1? "VARCHAR":"INTEGER");
					columnNames.add(keynameary[i]);
					if (keyvalueary[i].indexOf('\'') > -1)
						columnTypes.add(Types.VARCHAR);
					else
						columnTypes.add(Types.INTEGER);
				}

				insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(valarynocomm, keyvaluearynocomm);
				preparedStatement = selconn.prepareStatement(SQL_UPDATE);
				log.debug("record exist using update:{}", SQL_UPDATE);
				log.debug("record exist using valary:{} len={}", insvalary, insvalary.length);
				setValueps(preparedStatement, insvalary, usekey);
			} else {
				try {
				if (usekey)
					insvalary = com.systex.sysgateii.gateway.util.dataUtil.concatArray(keyvaluearynocomm, valarynocomm);
				else
					insvalary = valarynocomm;
				//20201116
				cnvInsertStr = generateActualSql(SQL_INSERT, (Object[])insvalary);
				//----
				log.debug("record not exist using select insert:{} toString=[{}]", SQL_INSERT, cnvInsertStr);
				} catch(Exception e) {
					e.printStackTrace();
					throw new Exception("format error");
				}
			}
			if (updateMode) {
				row = preparedStatement.executeUpdate();
				log.debug("executeUpdate() row=[{}]", row);
				if (keyvalueary.length > 0)
					rtnVal = keyvalueary;
				else {
					rtnVal = new String[1];
					rtnVal[0] = selkeyval;
				}
			} else {
				java.sql.Statement stmt2 = selconn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
				rs = ((java.sql.Statement) stmt2).executeQuery(cnvInsertStr);
				log.debug("executeUpdate()");
				int idx = 0;
				while (rs.next()) {
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
					for (int i = 0; i < keynameary.length; i++) {
						if (i == 0)
							rtnVal[idx] = rs.getString(keynameary[i]);
						else
							rtnVal[idx] = rtnVal[idx] + "," + rs.getString(keynameary[i]);
					}
					idx++;
				}
			}
			if (rs != null)
				rs.close();
			tblrs.close();
		}
		return rtnVal;
	}
	
	//20201028 delete
	//----
	//20210118 MatsudairaSyuMe delete change for vulnerability scanning sql injection defense
	public boolean DELETETB(String fromTblName, String keyname, String selkeyval)
			throws Exception {
		if (fromTblName == null || fromTblName.trim().length() == 0 || keyname == null || keyname.trim().length() == 0)
			throw new Exception("given table name or field or keyname error =>" + fromTblName);
		log.debug(String.format("delete table %s... where %s=%s", fromTblName, keyname, selkeyval));
		String[] keynameary = keyname.split(",");
		String[] keyvalueary = selkeyval.split(",");
		String[] valary = selkeyval.split(",");
		String deletesql = "DELETE FROM " + fromTblName + " WHERE ";

		if (keyname.indexOf(',') > -1 && selkeyval.indexOf(',') > -1) {
			keynameary = keyname.split(",");
			keyvalueary = selkeyval.split(",");
			if (keynameary.length != keyvalueary.length)
				throw new Exception("given fields keyname can't correspond to keyvfield =>keynames [" + keyname + "] selkayvals [" + selkeyval + "]");
			else {
				for (int i = 0; i < keynameary.length; i++)
					deletesql = deletesql + keynameary[i] + " = " + "?" + (i == (keynameary.length - 1) ? "" : " and ");
			}
		} else
			deletesql = deletesql + keyname + " = ?";
		log.debug("deletesql = [{}] ", deletesql);
		for (int i = 0; i < keyvalueary.length; i++) {
			int s = valary[i].indexOf('\'');
			int l = valary[i].lastIndexOf('\'');
			if (s != l && s >= 0 && l >= 0 && s < l)
				valary[i] = valary[i].substring(s + 1, l);
		}

		PreparedStatement stmt = selconn.prepareStatement(deletesql);
		for (int i = 0; i < keyvalueary.length; i++) {
			if (keyvalueary[i].indexOf('\'') > -1 )
				stmt.setString(i + 1, valary[i]);
			else
				stmt.setInt(i + 1, Integer.valueOf(valary[i]));
		}
		return stmt.execute();
	}
	private String generateActualSql(String sqlQuery, Object... parameters) throws Exception {
	    String[] parts = sqlQuery.split("\\?");
	    StringBuilder sb = new StringBuilder();

	    // This might be wrong if some '?' are used as litteral '?'
	    for (int i = 0; i < parts.length; i++) {
	        String part = parts[i];
	        sb.append(part);
	        if (i < parameters.length) {
	            sb.append(getValueps(i, (String[]) parameters));
	        }
	    }

	    return sb.toString();
	}
	private String formatParameter(Object parameter) {
	    if (parameter == null) {
	        return "NULL";
	    } else {
	        if (parameter instanceof String) {
	            return "'" + ((String) parameter).replace("'", "''") + "'";
	        } else if (parameter instanceof Timestamp) {
	            return "to_timestamp('" + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS").
	                    format(parameter) + "', 'mm/dd/yyyy hh24:mi:ss.ff3')";
	        } else if (parameter instanceof Date) {
	            return "to_date('" + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").
	                    format(parameter) + "', 'mm/dd/yyyy hh24:mi:ss')";
	        } else if (parameter instanceof Boolean) {
	            return ((Boolean) parameter).booleanValue() ? "1" : "0";
	        } else {
	            return parameter.toString();
	        }
	    }
	}
	//----
	private PreparedStatement setValueps(PreparedStatement ps, String[] updvalary, boolean fromOne) throws Exception {
		//20201119
		//fromOne true start from index 1 otherwise, false from index 0
		int type;
		String obj = "";
		int j = 1;
		if (!fromOne)
			j = 0;
		int i = 1;
		verbose = true;
		if (verbose)
			log.debug("j={} columnNames.size()={}",j,columnNames.size());
		for (; j < columnNames.size(); j++) {
			type = columnTypes.get(j);
			obj = updvalary[j];
			if (verbose)
				log.debug("\tj=" + j + ":[" + obj + "]");
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
	private String getValueps(int j, String[] updvalary) throws Exception {
		// updinsert true for update, false for insert
		String rtn = "";
		int type;
		String obj = "";
		verbose = true;
		type = columnTypes.get(j);
		obj = updvalary[j];
		if (verbose)
			log.debug("\tj=" + j + ":[" + obj + "]");
		switch (type) {
		case Types.VARCHAR:
		case Types.CHAR:
			if (verbose)
				log.debug("getString");
			rtn = "'" + obj + "'";
			break;
		case Types.DECIMAL:
			if (verbose)
				log.debug("getDouble");
			rtn = " " + obj + " ";
			break;
		case Types.TIMESTAMP:
			if (verbose)
				log.debug("getTimestamp[{}]", obj);
			rtn = "'" + obj + "'";
			break;
		case Types.BIGINT:
			if (verbose)
				log.debug("getLong");
			rtn = " " + obj + " ";
			break;
		case Types.BLOB:
			if (verbose)
				log.debug("getBlob");
			rtn = "'" + obj + "'";
			break;
		case Types.CLOB:
			if (verbose)
				log.debug("getClob");
			rtn = "'" + obj + "'";
			break;
		case Types.DATE:
			if (verbose)
				log.debug("getDate");
			rtn = "'" + obj + "'";
			break;
		case Types.DOUBLE:
			if (verbose)
				log.debug("getDouble");
			rtn = " " + obj + " ";
			break;
		case Types.INTEGER:
			if (verbose)
				log.debug("getInt/getInt");
			rtn = " " + obj + " ";
			break;
		case Types.NVARCHAR:
			if (verbose)
				log.debug("getNString(idx, v)");
			rtn = "'" + obj + "'";
			break;
		default:
			log.error("undevelop type:{} change to string", type);
			rtn = "'" + obj + "'";
			break;
		}
//		if (verbose)
//			System.out.println();
		return rtn;
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
	//20210118 MatsudairaSyuMe for vulnerability scanning sql injection defense
	private Connection getMySqlConnection(String url, String username, String password) throws Exception {
		String driver = "org.gjt.mm.mysql.Driver";
		//String url = "jdbc:mysql://localhost/demo2s";
		Class.forName(driver);
		Connection conn = DriverManager.getConnection(url, username, password);
		return conn;
	}

	//20210118 MatsudairaSyuMe for vulnerability scanning sql injection defense
	private Connection getOracleConnection(String url, String username, String password) throws Exception {
		String driver = "oracle.jdbc.driver.OracleDriver";
//		String url = "jdbc:oracle:thin:@localhost:1521:caspian";

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
		String fromurl = "jdbc:db2://172.16.71.141:50000/BISDB";
		String fromuser = "BIS_USER";
		String frompass = "bisuser";

		boolean verbose = true;
//		String fn = "BISAP.TB_AUDEVSTS";
		String fn = "BISAP.TB_AUDEVCMDHIS";
//		String fn = "BISAP.TB_AUDEVCMD";
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
//			total = jsel2ins.UPSERT("BISAP.TB_AUSVRPRM", "TBSDY", "01090910", "SVRID", "1");
//			String[] sno = jsel2ins.INSSELChoiceKey(fn, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CURSTUS", "1,1,'9838901','START','2020-10-21 09:46:38.368000','0'", "SNO", "-1", false, true);
			String[] sno = jsel2ins.INSSELChoiceKey(fn, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,CURSTUS", "1,1,'9838901','','2020-10-21 09:46:38.368000','START','2020-10-21 09:46:38.368000','2'", "SNO", "1030", false, true);
			for (int i = 0; i < sno.length; i++)
				System.out.println("sno[" + i + "] = ["+sno[i]+"]");
//			jsel2ins.DELETETB(fn, "SVRID,BRWS", "1,'9838901'");
//			jsel2ins.DELETETB(fn, "BRWS", "'9838901'");
			jsel2ins.CloseConnect();
			jsel2ins = null;
			System.out.println("total " + total + " records transferred");
			
/*
			if (jsel2ins == null)
				jsel2ins = new GwDao(fromurl, fromuser, frompass, verbose);
			String tb = "BISAP.TB_AUDEVCMD";
			System.out.println("table " + tb);
			String[] cmd = jsel2ins.SELMFLD(tb, "SVRID,BRWS,CMD,AUID,CMDCREATETIME,EMPNO", "SVRID", "1", true);
			for (String c: cmd)
				System.out.println(" cmd= " + c);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			String t = sdf.format(new java.util.Date());

			updValue = "'','START','" + t + "'";

			int row = jsel2ins.UPDT(tb, "CMD, CMDRESULT,CMDRESULTTIME", updValue, "SVRID,BRWS", "1,9838901");
			log.debug("total {} records update", row);

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
	}// en*/
}
