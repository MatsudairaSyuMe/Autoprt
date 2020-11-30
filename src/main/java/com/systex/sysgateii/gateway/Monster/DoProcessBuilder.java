package com.systex.sysgateii.gateway.Monster;

/******************
 * MatsudairaSyume
 * 20201119
 * Running the program
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoProcessBuilder {
	private static Logger log = LoggerFactory.getLogger(DoProcessBuilder.class);
	private String[] runArgs = null;

	DoProcessBuilder(String args[]) {
		runArgs = args;
	}

	public void Go() {
		if (runArgs.length <= 0) {
			log.info("Need command to run");
			System.exit(-1);
		}
		try {
			Process process = new ProcessBuilder(runArgs).start();
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;

			log.debug("Output of running {} is:", Arrays.toString(runArgs));
			while ((line = br.readLine()) != null) {
				log.debug(line);
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
