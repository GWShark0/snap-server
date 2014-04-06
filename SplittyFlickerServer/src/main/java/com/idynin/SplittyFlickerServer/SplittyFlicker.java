package com.idynin.SplittyFlickerServer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ProcessBuilder.Redirect;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.jhlabs.image.GaussianFilter;
import com.jhlabs.image.MotionBlurOp;

/**
 * Hello world!
 *
 */
public class SplittyFlicker
{
	static int splitToFlick(File inputImage, File outputVideo, int fps, int msLen,
			final int rows) throws IOException {
		final BufferedImage image;

		final int cols = rows;
		final int chunks = rows * cols;

		final int chunkWidth;
		final int chunkHeight;
		final BufferedImage imgs[];

		image = resizeImage(ImageIO.read(inputImage));

		chunkWidth = image.getWidth() / cols; // determines the chunk width
												// and height
		chunkHeight = image.getHeight() / rows;

		int count = 0;
		imgs = new BufferedImage[chunks]; // Image array to hold
											// image
		// chunks
		for (int x = 0; x < rows; x++) {
			for (int y = 0; y < cols; y++) {
				// Initialize the image array with image chunks
				imgs[count] = new BufferedImage(chunkWidth,
						chunkHeight, image.getType());

				// draws the image chunk
				Graphics2D gr = imgs[count++].createGraphics();
				gr.drawImage(image, 0, 0, chunkWidth, chunkHeight,
						chunkWidth * y, chunkHeight * x, chunkWidth
								* y + chunkWidth, chunkHeight * x
								+ chunkHeight, null);
				gr.dispose();
			}
		}

		long imagesNeeded = Math.round(fps * (msLen / 1000.0) + 1);

		BufferedImage temp;
		Graphics2D tempG;

		final List<Integer> l;
		Integer visitationOrder[] = new Integer[rows * cols];
		for (int i = 0; i < visitationOrder.length; i++) {
			visitationOrder[i] = i;
		}
		l = Arrays.asList(visitationOrder);
		Collections.shuffle(l);

		int cnt = 0, oldcnt = 0;

		BufferedImage blurred = deepCopy(image);

		GaussianFilter gausBlur = new GaussianFilter(88);

		// MotionBlurOp mbo = new MotionBlurOp(29, 47, 3.14f, 2);

		gausBlur.filter(image, blurred);

		List<Integer> used = new ArrayList<Integer>();
		used.addAll(l);

		while (--imagesNeeded > 0) {
			// temp = deepCopy(blurred);
			temp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
			tempG = temp.createGraphics();
			int im;
			tempG.setColor(Color.BLACK);

			if (cnt % l.size() < oldcnt) {
				Collections.shuffle(l);
			}

			used.addAll(l);
			for (int i = 0; i < (rows * cols * .40); i++) {
				im = l.get(cnt++ % l.size());
				tempG.drawImage(imgs[im],
						((im % rows) * chunkWidth),
						((im / cols) * chunkHeight),
						null);
				for (int j = 0; j < used.size(); j++) {
					if (used.get(j) == im) {
						used.remove(j);
					}
				}
			}
			// BufferedImage qr = toBufferedImage(makeColorTransparent(qrGen("someshit"),
			// Color.WHITE,
			// (short) 0));
			// qr = toBufferedImage(makeColorTransparent(qr, Color.BLACK, (short) 0xFF));
			// System.out.println(used.size());
			// for (int i : used) {
			// tempG.drawImage(qr,
			// ((i % rows) * chunkWidth),
			// ((i / cols) * chunkHeight),
			// null);
			// }
			File out = new File("./output/" + inputImage.getName() + "-out-"
					+ String.format("%04d", imagesNeeded) + ".jpg");

			out.mkdirs();
			// out.deleteOnExit();

			out.delete();

			ImageIO.write(temp, "jpg", out);
		}

		String ffmpeg = "/usr/local/bin/ffmpeg";
		String inputFilePattern = inputImage.getName() + "-out-%4d.jpg";
		String vidCodec = "libx264";
		String bitrate = "2800k";

		new File("output/out.mp4").delete();

		outputVideo.delete();
		ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-r", "" + fps, "-start_number", "1", "-i",
				inputFilePattern, "-c:v", vidCodec,
				"-b:v", bitrate,
				outputVideo.getCanonicalPath());
		pb.directory(new File("output"));
		pb.redirectOutput();
		pb.redirectError();
		System.out.println("Starting encoding");
		System.out.println(Arrays.toString(pb.command().toArray(new String[pb.command().size()])));
		Process p = pb.start();
		final BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		final BufferedReader brerr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

		Thread printer = new Thread() {
			public void run() {
				String line;
				try {
					while ((line = br.readLine()) != null) {
						System.out.println(line);
					}
					while ((line = brerr.readLine()) != null) {
						System.out.println(line);
					}
				} catch (Exception e) {
				}
			};
		};
		printer.setDaemon(true);
		printer.start();
		try {
			return p.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return 1;
	}

	static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	private static BufferedImage resizeImage(BufferedImage originalImage) {
		// 1136 x 640
		int maxWidth = 640, maxHeight = 1136;

		int newWidth = originalImage.getWidth(), newHeight = originalImage.getHeight();
		if (originalImage.getWidth() > maxWidth) {
			newWidth = maxWidth;
			newHeight = (maxWidth * originalImage.getHeight()) / originalImage.getWidth();
			if (newHeight > maxHeight) {
				newHeight = maxHeight;
				newWidth = (maxHeight * newWidth) / originalImage.getHeight();
			}
		} else if (originalImage.getHeight() > maxHeight) {
			newHeight = maxHeight;
			newWidth = (maxHeight * originalImage.getWidth()) / originalImage.getHeight();
			if (newWidth > maxWidth) {
				newWidth = maxWidth;
				newHeight = (maxWidth * originalImage.getHeight()) / originalImage.getWidth();
			}
		}

		if (newWidth % 2 != 0) {
			newWidth--;
		}

		if (newHeight % 2 != 0) {
			newHeight--;
		}

		BufferedImage resizedImage = new BufferedImage(newWidth, newHeight,
				originalImage.getType());
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
		g.dispose();

		return resizedImage;
	}

	private static BufferedImage qrGen(String input) {
		Charset charset = Charset.forName("UTF-8");
		CharsetEncoder encoder = charset.newEncoder();
		byte[] b = null;
		try {
			// Convert a string to UTF-8 bytes in a ByteBuffer
			ByteBuffer bbuf = encoder
					.encode(CharBuffer
							.wrap("utf 8 characters - i used hebrew, but you should write some of your own language characters"));
			b = bbuf.array();
		} catch (CharacterCodingException e) {
			System.out.println(e.getMessage());
		}

		String data;
		try {
			data = new String(b, "UTF-8");
			// get a byte matrix for the data
			BitMatrix matrix = null;
			int h = 10;
			int w = 10;
			com.google.zxing.Writer writer = new MultiFormatWriter();
			try {
				Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>(2);
				hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
				matrix = writer.encode(data,
						com.google.zxing.BarcodeFormat.QR_CODE, w, h, hints);
			} catch (com.google.zxing.WriterException e) {
				System.out.println(e.getMessage());
			}

			return MatrixToImageWriter.toBufferedImage(matrix);
		} catch (UnsupportedEncodingException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	/**
	 * Make provided image transparent wherever color matches the provided color.
	 *
	 * @param im
	 *            BufferedImage whose color will be made transparent.
	 * @param color
	 *            Color in provided image which will be made transparent.
	 * @return Image with transparency applied.
	 */
	public static Image makeColorTransparent(final BufferedImage im, final Color color,
			final short trans)
	{
		class CustomImageFilter extends RGBImageFilter
		{
			private int markerRGB;

			public CustomImageFilter(final Color color)
			{
				// the color we are looking for (white)... Alpha bits are set to opaque
				markerRGB = color.getRGB();// | 0xFFFFFFFF;
			}

			public int filterRGB(final int x, final int y, final int rgb)
			{
				if ((rgb | 0xFF000000) == markerRGB)
				{

					// Mark the alpha bits as zero - transparent
					int stuff = 0x00FFFFFF | (trans << 8 * 6);
					if (Color.BLACK == color) {
						System.out.println(new BigInteger("" + stuff).toString(2));
					}
					return stuff & rgb;
				}
				else
				{
					// nothing to do
					return rgb;
				}
			}
		}
		CustomImageFilter filter = new CustomImageFilter(color);
		FilteredImageSource ip = new FilteredImageSource(im.getSource(), filter);
		return Toolkit.getDefaultToolkit().createImage(ip);
	}

	public static BufferedImage toBufferedImage(Image img)
	{
		if (img instanceof BufferedImage)
		{
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null),
				BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}

	/**
	 * Was 'anonymous inner class' in Java version.
	 */

	// Thread averager = new Thread() {
	// public void run() {
	// byte[] pixels = ((DataBufferByte) image.getRaster()
	// .getDataBuffer()).getData();
	// long redBucket = 0;
	// long greenBucket = 0;
	// long blueBucket = 0;
	// long pixelCount = 0;
	//
	// for (int y = 0; y < pixels.length; y++) {
	// Color c = new Color(pixels[y]);
	// pixelCount++;
	// redBucket += c.getRed();
	// greenBucket += c.getGreen();
	// blueBucket += c.getBlue();
	// // does alpha matter?
	//
	// }
	// Color averageColor = new Color(
	// (int) (redBucket / pixelCount),
	// (int) (greenBucket / pixelCount),
	// (int) (blueBucket / pixelCount));
	//
	// ImagePanel.this.setBackground(averageColor);
	// };
	// };
}
