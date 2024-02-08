import util.AntiAliasing;
import util.Coordinates;
import util.Translate;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	int width = 512;
	int height = 512;
	private static final int SCREEN_WIDTH = 600;
	private static final int SCREEN_HEIGHT = 600;
	private long INITIAL_ANGLE = 0;
	private double INITIAL_SCALE = 1;
	private int inputAngle;
	private double inputScale;
	private int inputFrameRate;
	int[][] originalPixelMatrix = new int[height][width];

	class DoubleBuffering implements Callable<BufferedImage>{

		@Override
		public BufferedImage call() throws Exception {
			return animate();
		}
	}
	private void readImageRGB(int width, int height, String imgPath)
	{

		try
		{
			int frameLength = width*height*3;
			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);
			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2];
					originalPixelMatrix[x][y] = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					ind++;
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	private BufferedImage animate() throws Exception {
		BufferedImage imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				Coordinates translated = Translate.coordinateSys(new Coordinates(x, y), height, width);
				double[][] rotated = util.MatrixUtil.rotationAndScale(INITIAL_ANGLE + 180, translated.getxCoordinate(), translated.getyCoordinate(), INITIAL_SCALE);
				translated = Translate.coordinatePixel(new Coordinates(rotated[0][0], rotated[1][0]), height, width);
				if (!(translated.getxCoordinate() >= height || translated.getyCoordinate() >= width
						|| translated.getxCoordinate() < 0 || translated.getyCoordinate() < 0)){
					int pix = 0;
					if(INITIAL_SCALE < 1) pix = AntiAliasing.averagingFilter(translated, originalPixelMatrix);
					else pix = originalPixelMatrix[(int) translated.getxCoordinate()][(int) translated.getyCoordinate()];
					imgOne.setRGB(x, y, pix);
				} else{
					imgOne.setRGB(x, y, Integer.MAX_VALUE);
				}
			}
		}
		return imgOne;
	}

	public void showIms(String[] args) throws Exception {
		if(args.length != 4){
			throw new Exception("Invalid number of arguments");
		}
		String image = args[0];
		inputScale = Double.parseDouble(args[1]);
		inputAngle = Integer.parseInt(args[2]);
		inputFrameRate = Integer.parseInt(args[3]);
		readImageRGB(width, height, image);
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		run();
	}
	public void run() throws Exception {

		lbIm1 = new JLabel();
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		lbIm1.setHorizontalAlignment(JLabel.CENTER);
		frame.getContentPane().add(lbIm1, c);
		frame.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
		frame.pack();
		frame.setVisible(true);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		boolean isDouble = false;
		boolean gameLoop = true;
		Future<BufferedImage> future1 = null;
		Future<BufferedImage> future2 = null;
		BufferedImage imgOne = null;

		while (gameLoop) {
				INITIAL_ANGLE += inputAngle;
				INITIAL_SCALE *= inputScale;
				if (!isDouble) {
					if (future1 == null) {
						future1 = executor.submit(new DoubleBuffering());
					}
					imgOne = future1.get();
					future2 = executor.submit(new DoubleBuffering());
				} else {
					imgOne = future2.get();
					future1 = executor.submit(new DoubleBuffering());
				}
				lbIm1.setIcon(new ImageIcon(imgOne));
				try {
					Thread.sleep(1000/inputFrameRate);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				isDouble = !isDouble;
		}
		executor.shutdown();
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		try {
			ren.showIms(args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
