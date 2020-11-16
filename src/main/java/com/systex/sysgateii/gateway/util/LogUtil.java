package com.systex.sysgateii.gateway.util;

import ch.qos.logback.classic.Level;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * <pre>
 * Change log4j log file name at initial.
 * 會影響全部的使用者
 * sample:
 *   LogUtil.getDailyLogger("pathname", "logfilename", "debug", "TIME     [0000]:%d{yyyy.MM.dd HH:mm:ss:SSS} %msg%n");
 * </pre>
 * @param logPathName
 * @param logFileName
 * @parm logLevel
 * @param logMessagePattrn
 */

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.util.FileSize;


public class LogUtil {
	public static Logger getDailyLogger(String pathname, String logName, String level, String ptrn) {
		Logger logbackLogger = (Logger) LoggerFactory.getLogger(logName);
		RollingFileAppender<ILoggingEvent> a = (RollingFileAppender<ILoggingEvent>) ((AppenderAttachable<ILoggingEvent>) logbackLogger).getAppender(logName);
		if (a != null)
		{
//			System.out.println("Log Appender already exist");
			return logbackLogger;
		}		

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

//		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//		String byDate = sdf.format(new Date());

		RollingFileAppender<ILoggingEvent> rfAppender = new RollingFileAppender<ILoggingEvent>();
		rfAppender.setContext(loggerContext);
//		rfAppender.setFile(logName + byDate + ".log");
		String fpn = "";
		if (pathname != null && pathname.trim().length() > 0)
			fpn = pathname + File.separator + logName + ".log";
		else
			fpn = "." + File.separator + logName + ".log";
		rfAppender.setFile(fpn);

//mark20200430		TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
		FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
		rollingPolicy.setContext(loggerContext);
		// rolling policies need to know their parent
		// it's one of the rare cases, where a sub-component knows about its parent
		rollingPolicy.setParent(rfAppender);
//		rollingPolicy.setFileNamePattern(logName + byDate + ".%i.log.zip");
		if (pathname != null && pathname.trim().length() > 0)
			fpn = pathname + File.separator + "archive" + File.separator + logName + "-%d{yyyy-MM-dd-HH-mm}.%i.log.zip";
		else
			fpn = "." + File.separator + "archive" + File.separator + logName + "-%d{yyyy-MM-dd-HH-mm}.%i.log.zip";

		rollingPolicy.setFileNamePattern(fpn );
//mark20200430		rollingPolicy.setMaxHistory(5);
//mark20200430		rollingPolicy.setCleanHistoryOnStart(true);
		rollingPolicy.start();

		SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<ILoggingEvent>();
//mark20200430		SizeAndTimeBasedFNATP<ILoggingEvent> triggeringPolicy = new SizeAndTimeBasedFNATP<ILoggingEvent>();
		triggeringPolicy.setContext(loggerContext);
		triggeringPolicy.setMaxFileSize(FileSize.valueOf("30MB"));
//mark20200430		triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy);
		triggeringPolicy.start();

		//---
		//add for SizeAndTimeBasedFNATP
//mark20200430		rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(triggeringPolicy);
//mark20200430		rollingPolicy.start();
		//---

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
//		encoder.setPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n");
		encoder.setPattern(ptrn);
		encoder.start();

		rfAppender.setEncoder(encoder);
		rfAppender.setRollingPolicy(rollingPolicy);
		rfAppender.setTriggeringPolicy(triggeringPolicy);

		rfAppender.start();

		// attach the rolling file appender to the logger of your choice
		logbackLogger = loggerContext.getLogger(logName);
		logbackLogger.addAppender(rfAppender);
		if (level.equalsIgnoreCase("debug"))
		{
			logbackLogger.setLevel(Level.DEBUG);			
		}
		else if (level.equalsIgnoreCase("info"))
		{
			logbackLogger.setLevel(Level.INFO);						
		}
		else if (level.equalsIgnoreCase("error"))
		{
			logbackLogger.setLevel(Level.ERROR);						
		}
		else
		{
			logbackLogger.setLevel(Level.ALL);												
		}

		return logbackLogger;
	}
}
