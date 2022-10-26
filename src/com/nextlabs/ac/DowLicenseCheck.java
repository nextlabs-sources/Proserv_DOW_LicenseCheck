package com.nextlabs.ac;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.pf.domain.destiny.serviceprovider.IFunctionServiceProvider;
import com.bluejungle.pf.domain.destiny.serviceprovider.ServiceProviderException;
import com.nextlabs.helper.PluginConstants;
import com.nextlabs.helper.HSQLHelper;

public class DowLicenseCheck implements IFunctionServiceProvider{
	
	private static final Log log = LogFactory.getLog(DowLicenseCheck.class);
	
	String connectionUrl;
	String userName;
	String password;
	
	HSQLHelper hsqlHelper;
	
	public DowLicenseCheck() {
		init();
	}
	
	@Override
	public void init(){
		log.info("DAC : " + "DowLicenseCheck Advanced Condition initialized.");
	}

	@Override
	public IEvalValue callFunction(String functionName, IEvalValue[] args)
			throws ServiceProviderException {
		log.info("DAC : " + "DowLicenseCheck callFunction() start.");
		long lserviceCurrentTime = System.nanoTime();
		
		IEvalValue result = EvalValue.build(0);
		
		if("getCount".equalsIgnoreCase(functionName)) {
			result = getCount(args);
		}
		
		log.info("DAC : " + "DowLicenseCheck callFunction() End. Returning - " + result.getValue().toString());
		log.info("DAC : " + "Advanced condition took : " + (System.nanoTime() - lserviceCurrentTime)/1000000 + " ms");
		return result;
	}
	
	private String findInstallFolder() {
		File f = new File(".");
		String path = f.getAbsolutePath();
		return path;		
	}
	
	private IEvalValue getCount(IEvalValue[] args) {
		log.info("DAC : " + "DowLicenseCheck getCount() start.");

		/*
		 * Check if we have both arguments.
		 */
		
		if(args.length<2) {
			log.info("DAC : " + "Minimum number of parameters = 2");
			return EvalValue.build(0);
		}
		
		/*
		 * If any argument is null, return DENY.
		 */
		
		if(args[0].getValue()==null || args[1].getValue()==null) {
			return EvalValue.build(0);
		}
		
		/*
		 * Query HSQL.
		 */
		
		String user = args[0].getValue().toString();
		String eccn = args[1].getValue().toString();
		
		log.info("DAC : " + "Opening HSQL connection.");
		Properties hsqlProps = new Properties();
		try {
			PluginConstants.INSTALL_LOC = findInstallFolder();
			String path = PluginConstants.INSTALL_LOC + PluginConstants.PC_ROOT;
			FileInputStream file = new FileInputStream(path + "\\config\\DowAdvancedCondition.PROPERTIES");
			log.info("DAC : " + "PATH : " + path + "\\config\\DowAdvancedCondition.PROPERTIES");
			hsqlProps.load(file);
			hsqlHelper = new HSQLHelper(hsqlProps.getProperty("hsql_server_url"),hsqlProps.getProperty("hsql_user_name"),hsqlProps.getProperty("hsql_password"));
		} catch (FileNotFoundException e1) {
			log.error("DAC : " + "Cannot find properties file." , e1);
			return EvalValue.build(0);
		} catch (IOException e) {
			log.error("DAC : " + "Cannot find properties file." , e);
			return EvalValue.build(0);
		}
		log.info("DAC : " + "Opened HSQL connection.");
		
		Connection hsqlConn = null;
		Statement stmt = null;
		
		try {
			
			hsqlConn = hsqlHelper.openConnection();
			stmt = hsqlConn.createStatement();
			
			String licQuery = "SELECT COUNT(*) AS NUMLIC FROM LOADB INNER JOIN USERLIC ON USERLIC.LICENSE=LOADB.LICENSE AND USERLIC.UID='" + user.toLowerCase() + "' WHERE LOADB.ECCN='"+ eccn.toLowerCase() +"' AND LOADB.EXPIRY>=CURDATE() AND LOADB.EFFECTIVE<=CURDATE();";
			String loaQuery = "SELECT COUNT(*) AS NUMLIC FROM LOADB INNER JOIN USERLOA ON USERLOA.LOA=LOADB.LOA WHERE LOADB.ECCN='"+ eccn.toLowerCase() +"' AND LOADB.EXPIRY>=CURDATE() AND LOADB.EFFECTIVE<=CURDATE() AND USERLOA.UID='"+ user.toLowerCase() +"';";
			
			log.info("DAC : " + "LIC SQL - " + licQuery);
			log.info("DAC : " + "LOA SQL - " + loaQuery);
			
			ResultSet licQueryRs = stmt.executeQuery(licQuery);
			ResultSet loaQueryRs = stmt.executeQuery(loaQuery);
			
			int licCount = 0;
			int loaCount = 0;
			
			while(licQueryRs.next()){
				licCount = Integer.parseInt(licQueryRs.getString("NUMLIC"));
		    }
			
			if(licCount>0) {
				log.info("DAC : " + "Got " + String.valueOf(licCount) + " matching licenses. Not checking LOA matches.");				
			} else {
				while(loaQueryRs.next()){
					loaCount = Integer.parseInt(loaQueryRs.getString("NUMLIC"));
			    }				
				log.info("DAC : " + "Got " + String.valueOf(loaCount) + " matching LOA");
			}
			
			return EvalValue.build(licCount + loaCount);
				
		} catch (SQLException e) {
			log.error("DAC : " + "Error querying HSQL DB", e);
		} finally {
			if(stmt!=null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					log.error("DAC : " + "Error closing stmt.", e);
				}
			}
			if(hsqlConn!=null) {
				try {
					hsqlConn.close();
				} catch (SQLException e) {
					log.error("DAC : " + "Error closing conn.", e);
				}
			}
		}
				
		log.info("DAC : " + "DowLicenseCheck getCount() end.");
		return EvalValue.build(0);
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		DowLicenseCheck licenseCheck = new DowLicenseCheck();
		licenseCheck.init();
		
		IEvalValue[] sDataArr = new IEvalValue[2];
		sDataArr[0] = EvalValue.build("lee.adams"); //user sid
		sDataArr[1] = EvalValue.build("123abc");
		
		licenseCheck.callFunction("getCount", sDataArr);
	}

}
