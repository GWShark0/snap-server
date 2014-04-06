package com.idynin.SplittyFlickerServer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class SplittyRunner {
	public static void main(String[] args) throws IOException {
		if (args.length == 2) {
			FileUtils.deleteDirectory(new File("output"));
			File inputImage = new File(args[0]);
			File outputVid = new File(args[1]);
			outputVid.delete();
			System.exit(SplittyFlicker.splitToFlick(inputImage, outputVid, 60, 10000, 6));
		} else {
			System.err.println("args bitch");
		}
	}
}
