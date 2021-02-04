import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.frame.Recorder;
import ij.gui.GenericDialog;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import javax.swing.JLabel;

import loci.plugins.BF;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import loci.plugins.in.ImporterPrompter;
import loci.formats.gui.BufferedImageReader;
import loci.formats.FormatException;
import java.awt.image.BufferedImage;
import loci.formats.gui.AWTImageTools;

public class RML_ implements PlugIn, MouseListener {

	private BufferedImageReader thumbReader;
	private ImporterOptions options;
	private ImportProcess process;
	private ImagePlus imp;
	
	private static final String[] formats = {".lif",".czi",".ets"};

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Specify file");
		gd.addStringField("path", "");
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		String path = gd.getNextString();
		if ( isSupportedExt(path)) {
			try {
				options = new ImporterOptions();
				options.setId(path);
				options.setFirstTime(false);
				options.setUpgradeCheck(false);
				process = new ImportProcess(options);
				new ImporterPrompter(process);
				process.execute();
				options = process.getOptions();
				options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
				options.setColorMode("Composite");
				int count = process.getSeriesCount();
				thumbReader = new BufferedImageReader(process.getReader());
				int sx = thumbReader.getThumbSizeX();
				int sy = thumbReader.getThumbSizeY();
				String title = path.substring(path.lastIndexOf(File.separator) + 1);
				imp = IJ.createImage("..." + title, "RGB black", sx, sy + 60, count);
				ImageStack stack = imp.getStack();
				for (int i = 0; i < count; i++) {
					thumbReader.setSeries(i);
					String label = process.getSeriesLabel(i);
					String label2 = " " + label.replaceAll("[:;]", "\n");
					imp.setSlice(i + 1);
					int z = thumbReader.getSizeZ() / 2;
					int t = thumbReader.getSizeT() / 2;
					int ndx = thumbReader.getIndex(z, 0, t);
					Exception exc = null;
					try {
						BufferedImage thumb = thumbReader.openThumbImage(ndx);
						thumb = AWTImageTools.autoscale(thumb);
						ImagePlus im = new ImagePlus("slice", thumb);
						IJ.run(im, "RGB Color", "");
						ImageProcessor ip = stack.getProcessor(i + 1);
						ip.setColor(new Color(255, 255, 255));
						ip.fill();
						ip.insert(im.getProcessor(), 0, 0);
						ip.setFont(new Font("SansSerif", Font.BOLD, 10));
						ip.setColor(new Color(255, 0, 0));
						ip.setAntialiasedText(true);
						ip.drawString(label2, 5, sy + 14);
						stack.setProcessor(ip, i + 1);
					} catch (FormatException e) {
						exc = e;
					} catch (IOException e) {
						exc = e;
					}
					if (exc != null) {
						IJ.error(exc.toString());
					}
				}
				imp.setStack(stack);
				imp.setSlice(1);
				imp.show();
				ImageWindow win = imp.getWindow();
				ImageCanvas canvas = win.getCanvas();
				canvas.addMouseListener(this);
			} catch (FormatException e) {
				e.printStackTrace();
				IJ.log("Bioformats had problems reading this file.");
			} catch (IOException e) {
				e.printStackTrace();
				IJ.log("Bioformats had problems reading this file.");
			}
		}
	}
	
	private boolean isSupportedExt(String s) {
		boolean supported = false;
		for (String ext : formats)
			if (s.toLowerCase().endsWith(ext)) supported=true;
		return supported;
	}

	public void mouseEntered(MouseEvent me) {
	}

	public void mouseExited(MouseEvent me) {
	}

	public void mousePressed(MouseEvent me) {
	}

	public void mouseReleased(MouseEvent me) {
	}

	public void mouseClicked(MouseEvent me) {
		if (me.getClickCount() == 2) {
			if ((IJ.getToolName() == "zoom") && (!IJ.spaceBarDown())) {
				IJ.showStatus("<!> Use SPACE+double click to open series, or change tools");
				return;
			}
			int slice = imp.getCurrentSlice();
			options.clearSeries();
			options.setSeriesOn(slice - 1, true);
			options.setAutoscale(true);
			try {
				ImagePlus[] imps = BF.openImagePlus(options);
				imps[0].show();
				if (Recorder.record) {
					String cmd = "run('Bio-Formats Importer', 'open=[";
					cmd += options.getId();
					cmd += "] autoscale color_mode=Composite rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT";
					cmd += " series_" + slice + "');\n";
					Recorder.recordString(cmd);
				}

			} catch (FormatException e) {
				e.printStackTrace();
				IJ.log("Bioformats had problems reading this file.");
			} catch (IOException e) {
				e.printStackTrace();
				IJ.log("Bioformats had problems reading this file.");
			}
		}
	}
}
