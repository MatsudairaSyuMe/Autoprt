package com.systex.sysgateii.gateway.Monster;

/******************
 * MatsudairaSyume
 * 20201119
 * Running the program
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.gateway.util.StrUtil;

public class DoProcessBuilder {
	private static Logger log = LoggerFactory.getLogger(DoProcessBuilder.class);
	private String[] runArgs = null;
	private static final String TrustedCmd = "bin/autosvr";
	private static final String[] TrustedArg1 = {"start", "stop", "restart"};
	private static final String TrustedArg2 = "--svrid";

	DoProcessBuilder(String args[]) {
		runArgs = args;
	}

	public void Go() {
		if (runArgs.length <= 0) {
			log.info("Need command to run");
			System.exit(-1);
		}
		try {
			//20210111 for vulnerability scanning command injection defense
			boolean chkOk = false;
			if (runArgs.length >= 4) {
				if (runArgs[1] != null && runArgs[1].trim().length() != 0) {
					for (String chkS : TrustedArg1) {
						log.debug("Output of runArgs[1]running is :{} chkOk={} chkS={}", runArgs[1], chkOk, chkS);
						if (chkS.equals(runArgs[1].trim())) {
							chkOk = true;
							break;
						}
					}
				}
				if (chkOk == true) {
					chkOk = false;
					if (runArgs[3] != null && runArgs[3].trim().length() != 0 && StrUtil.isNumeric(runArgs[3].trim()))
						chkOk = true;
				}
			}
			log.debug("Output of runArgs[0]={} {} runArgs[2]={} {} chkOk={}", runArgs[0], TrustedCmd,
					runArgs[2], TrustedArg2,chkOk);
			if (runArgs.length >= 4 && chkOk && runArgs[0].trim().equals(TrustedCmd)
					&& runArgs[2].trim().equals(TrustedArg2)) {
//				Process process = new ProcessBuilder(runArgs).start();
				ProcessBuilder pb = new ProcessBuilder(runArgs);
				Map<String, String> env = pb.environment();
				String currentDir = System.getProperty("user.dir");
				pb.directory(new File(currentDir));
				Process process = pb.start();
				//Process process = new ProcessBuilder(runArgs).start();
				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;

				log.debug("Output of running {} is:", Arrays.toString(runArgs));
				while ((line = br.readLine()) != null) {
					log.debug(line);
				}
			} else {
				log.error("!!!! Command {} ERROR !!!!", Arrays.toString(runArgs));
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("fork process error [{}]", e.toString());
		}
	}

	public static void main(String args[]) throws IOException {
        DoProcessBuilder dp = new DoProcessBuilder(args);
        dp.Go();

        /*
		if (args.length <= 0) {
			System.err.println("Need command to run");
			if (log != null)
				log.error("Need command to run");
			System.exit(-1);
		}

		Process process = new ProcessBuilder(args).start();
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;

		System.out.printf("Output of running %s is:", Arrays.toString(args));
		if (log != null)
			log.debug("Output of running {} is:", Arrays.toString(args));

		while ((line = br.readLine()) != null) {
			System.out.println(line);
			if (log != null)
				log.debug("{}", Arrays.toString(args));
		}
		*/
	}
}
