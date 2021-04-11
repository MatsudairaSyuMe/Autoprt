package com.systex.sysgateii.gateway.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
//import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;

public class LogTestMain {
	public static void main(String[] args) {
/*		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		RollingFileAppender<ILoggingEvent> rfAppender = new RollingFileAppender<ILoggingEvent>();
		rfAppender.setContext(loggerContext);
		rfAppender.setFile("testFile.log");
		
		FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
		rollingPolicy.setContext(loggerContext);
		// rolling policies need to know their parent
		// it's one of the rare cases, where a sub-component knows about its parent
		rollingPolicy.setParent(rfAppender);
		rollingPolicy.setFileNamePattern("testFile.%i.log.zip");
		rollingPolicy.start();

		SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<ILoggingEvent>();
		triggeringPolicy.setMaxFileSize(FileSize.valueOf("5MB"));
		triggeringPolicy.start();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n");
		encoder.start();

		rfAppender.setEncoder(encoder);
		rfAppender.setRollingPolicy(rollingPolicy);
		rfAppender.setTriggeringPolicy(triggeringPolicy);

		rfAppender.start();

		// attach the rolling file appender to the logger of your choice
		Logger logbackLogger = loggerContext.getLogger("LogTestMain");
		logbackLogger.addAppender(rfAppender);
*/
		// OPTIONAL: print logback internal status messages
//		StatusPrinter.print(loggerContext);

		// log something
		Logger logbackLogger = LogUtil.getDailyLogger("/home/scotthong/tmp", "LogTestMain", "info", "[%d{yyyy/MM/dd HH:mm:ss:SSS}]%msg%n");
		Logger logbackLogger2 = LogUtil.getDailyLogger("/home/scotthong/tmp", "LogTestMain2", "info", "[%d{yyyy/MM/dd HH:mm:ss:SSS}]%msg%n");
		for (int i = 0; i < 1000; i++) {
			logbackLogger.info("hello");
			logbackLogger.info("hello [{}]", i);
			logbackLogger2.info("hello");
			logbackLogger2.info("hello [{}]", i);
		}

	}
}
